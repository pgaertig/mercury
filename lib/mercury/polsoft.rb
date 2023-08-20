require 'json'
require 'jbuilder'
require 'csv'

class Mercury::Polsoft
  enable_logger


  def self.configure(config, output: nil)
    unless @write_transport
      @write_transport =
        pl.amitec.mercury.transport.FilesystemTransport.configure(config, false, 'iso-8859-2')
      #Mercury::Transport::Filesystem.configure(config, readonly: false, mode: 'r:iso-8859-2:utf-8')
    end
    unless @redbay_client
      @redbay_client = Mercury::RedbayClient.configure(config)
    end
    cache = Mercury::HashCache.new('data/mercury-hash-cache')
    new(config: config, write_transport: @write_transport, redbay_client: @redbay_client, cache: cache)
  end

  def initialize(config:, transport: nil, write_transport:, redbay_client:, cache: Mercury::NullCache.new)
    @config = config
    @transport = transport
    @write_transport = write_transport
    @redbay_client = redbay_client
    @hash_cache = cache
  end

  def watch
    #psftp = Mercury::PolsoftFtp.new
    psftp = pl.amitec.mercury.providers.polsoft.PolsoftFtp.configure(@config)
    while true do
      sync_orders
      psftp.syncDirToRemote("data/IMPORT_ODDZ_1", "data/IMPORT_ODDZ_1-sent", "/IMPORT_ODDZ_1")
      state = psftp.wait_for_content_pull("/EKSPORT_ODDZ_1")
      if(state)
        sync(state.transport)
        psftp.failureCleanup(state) #FIXME keeps dir
      end
      sleep 60
    end
  end

  def sync(transport)
    @redbay_client.session do
      dept = 1
      dept_dir = transport.subdir(".")
      #binding.pry
      raise "Incomplete directory" unless is_complete?(dept_dir)
      variant_source_ids = sync_variants(dept_dir, dept)
      sync_clients(dept_dir, dept, variant_source_ids: variant_source_ids)
      #sync_invoices(dept_dir, dept)
    end
  end

  def sync_selected_clients(transport, source_ids)
    @redbay_client.session do
      dept = 1
      dept_dir = transport.subdir(".")
      raise "Incomplete directory" unless is_complete?(dept_dir)
      sync_clients(dept_dir, dept, selected_source_ids: source_ids)
    end
  end

  # FIXME mm
  def _departments
    #settings = JSON.parse(@transport.read('settings.json'))
    #settings['departments']
    return 1
  end

  def is_complete?(transport)
    if transport.exists?('toppc.txt') && transport.read('toppc.txt') == "KONIEC"
      logger.debug("Data available")
      return true
    else
      logger.info("No data available #{transport}")
      return false
    end
  end

  def from_csv(str)
    pl.amitec.mercury.util.CSVToMaps.new.stream(str).toList()
    #CSV.parse(str, headers: true, col_sep: "\t", quote_char: '|')
  end

  def sync_variants(dept_dir, dept)
    products = from_csv(dept_dir.read("towary.txt"))
    stocks = from_csv(dept_dir.read("stany.txt"))
    producers = from_csv(dept_dir.read("produc.txt"))
    groups = from_csv(dept_dir.read("grupy.txt"))

    warehouse = @redbay_client.get_warehouse('polsoft', dept) or raise "No warehouse #{dept} in Redbay"
    warehouse_id = warehouse['id']

    def map_row(src, kk, vk)
      src.inject({}){ |result, row| result[row[kk]]=row[vk]; result }
    end

    stocks = map_row(stocks,'towar_numer', 'towar_ilosc')
    producers = map_row(producers, 'prd_numer', 'prd_nazwa')
    groups = map_row(groups, 'categories_id', 'categories_name') ## język? (language_id)

    #product = products.first
    stats = {products: 0, success:0, failed:0, cached: 0}
    variant_source_ids = Set.new

    products.each do |product|
      stats[:products] += 1
      code = product["towar_kod"]

      if code.nil? || code.empty?
        stats[:failed] += 1
        next
      end

      source_id = product["towar_numer"]

      variant_source_ids << source_id

      json = Jbuilder.encode do |json|
        json.code code
        json.product_code code

        json.source 'polsoft'
        json.source_id source_id

        json.ean product["towar_ean_sztuka"]
        json.unit product["tw_jm"]
        json.tax "#{product["towar_vat"]}%"
        #json.status "Y" #Active, or D - Deleted, or N - inactive
        json.lang "pl" #Default language
        #json.debug Time.now.to_s

        producer_id = product["towar_producent"]
        producer = producers[producer_id]
        if producer
          json.producer({source_id: producer_id, name: producer, source: "polsoft"})
        else
          json.producer nil
        end

        json.name({pl: product["towar_nazwa"]})

        categories = [
          [{source_id: product["nr_grupy"], name: {pl: groups[product["nr_grupy"]]}}]
        ]
        addcats = product["rodzaj"] # TODO: zaczytac rodz_tow.txt, jak jest jedna to zignorować
        json.categories categories

        czynna = product["substancja_czynna"]
        attrs = [
          {name: "GRATIS", value: product["towar_gratis"], lang: "pl"},
          {name: "ZBIORCZE", value: product["towar_ilosc_opak_zb"], lang: "pl"}
        ]

        attrs << {name: "SUBSTANCJA CZYNNA", value: czynna } if czynna

        json.attrs attrs

        json.stocks [{
                       source_id: "#{dept}:#{source_id}",
                       source: "polsoft",
                       warehouse_id: warehouse_id,
                       quantity: stocks[source_id] || 0,
                       price: product["towar_cena1"]
                     }]
      end

      payload = json.to_s

      begin
        same_data = @hash_cache.hit?(tenant: @config['tenant'],
                                     source: 'ps',
                                     resource: 'p',
                                     key: "#{dept}:#{source_id}",
                                     data: payload) do
          Mercury::ExponentialBackoff.run {
            @redbay_client.import_variant(payload)
          }
          logger.debug("Sent: #{payload}")
        end
        if same_data
          stats[:cached] += 1
          logger.debug("Cached: #{payload}")
        end
        stats[:success] += 1
        logger.debug("Stats: #{stats}")
      rescue => e
        stats[:failed] += 1
        logger.error("Failed send, #{e.inspect}")
      end
    end
    logger.info("Stats: #{stats}")
    variant_source_ids
  end

  def sync_clients(dept_dir, dept, variant_source_ids: nil, selected_source_ids: nil)
    stats = {clients: 0, success:0, failed:0, cached: 0, stock_discounts: 0}
    return unless is_complete?(dept_dir)

    return unless dept_dir.exists?("klienci.txt")
    return unless dept_dir.exists?("rabaty.txt")

    clients_reader = dept_dir.reader("klienci.txt")
    discounts_reader = dept_dir.reader("rabaty.txt")
    clientDiscountsStream = pl.amitec.mercury.providers.polsoft.ClientDiscountStreamer.new.stream(clients_reader, discounts_reader)
    #logger.debug("klienci.txt: loaded #{clients.length} clients")
    #discounts = from_csv(dept_dir.read("rabaty.txt"))
    #logger.debug("rabaty.txt: loaded #{discounts.length} discounts")
    #unknown_variants = Set.new


#    discounts_by_client = discounts.inject({}) do |result, discount|
    #      id = discount['kt_numer']
    #  client_discounts = result[id] || []
    #  variant_source_id = discount['towar_numer']
    #  if !variant_source_ids || variant_source_ids.include?(variant_source_id)
    #    client_discounts << {source_id: "#{dept}:#{variant_source_id}", price: discount['towar_cena_kontrah']}
    #    result[id] = client_discounts
    #  else
    #    unknown_variants << variant_source_id
    #  end
    #  result
    #end


    #unless unknown_variants.empty?
    #  logger.warn("Unkown product IDs for discount: #{unknown_variants}")
    #end

    clientDiscountsStream.iterator.each do |clientWithDiscounts|
      client = clientWithDiscounts.client
      id = client['kt_numer']
      if selected_source_ids && !selected_source_ids.include?(id)
        next
      end
      json = Jbuilder.encode do |json|
        json.source_id id
        json.source 'polsoft'
        json.name "#{client['kt_nazwa']} #{client['kt_nazwa_pom']}".strip
        json.email client['kt_email']
        json.phone client['kt_telefon']&.strip
        json.street client['kt_ulica']
        json.postcode client['kt_kod_pocztowy']
        json.city client['kt_miasto']
        json.province pl.amitec.mercury.dict.PostCodes.instance.code_to_province(client['kt_kod_pocztowy'])
        json.nip client['kt_nip']
        json.country 'PL'

        json.properties do
          json.iph_department dept
          json.iph_pricetype client['kt_rodzaj_ceny']
          json.iph_discount client['kt_rabat_auto']
          json.iph_debt client['kt_zadluzenie']
          json.iph_sector client['kategoria_1']
        end

        discounts = clientWithDiscounts.discounts.to_h
        if !discounts.empty?
          discounts_mapped = discounts.map{|k,v|
            {source_id: "#{dept}:#{k}", price: v}
          }
          json.stock_discounts discounts_mapped
          stats[:stock_discounts] += discounts_mapped.size
        end
      end

      payload = json.to_s

      begin
        same_data = @hash_cache.hit?(tenant: @config['tenant'],
                                     source: 'ps',
                                     resource: 'c',
                                     key: "#{dept}:#{id}",
                                     data: payload) do
          #Mercury::ExponentialBackoff.run {
            @redbay_client.import_company(payload)
          #}
          logger.debug("Sent: #{payload}")
        end
        if same_data
          stats[:cached] += 1
          logger.debug("Cached: #{payload}")
        end
        stats[:success] += 1
        logger.debug("Stats: #{stats}")
      rescue => e
        stats[:failed] += 1
        logger.error [e.message, *e.backtrace].join($/)
        logger.warn("Stats: #{stats}")
      end

    end
    ensure
      clients_reader.close
      discounts_reader.close
  end

  def sync_invoices(dept_dir, dept)
    stats = {invoices: 0, clients: 0, success:0, failed:0, ignored: 0 }
    return unless is_complete?(dept_dir)
    return unless dept_dir.exists?("rozrach.txt")

    invoices = from_csv(dept_dir.read("rozrach.txt"))

    invoices.each do |invoice|
      number = invoice['symbol_fakt']
      company_source_id = invoice['kt_numer_platnik']
      json = Jbuilder.encode do |json|
        json.number number
        json.netto invoice['wartosc_netto']
        json.generated Date.strptime(invoice['data_wyst'],"%Y.%m.%d")
        json.payment Date.strptime(invoice['data_platn'],"%Y.%m.%d")
        json.paid invoice['data_ost_splaty']
        json.deposit invoice['dlug']
        json.brutto invoice['kwota']

        #json.source_id invoice['nr_fakt']
        #type_id = invoice['typ_dokumentu']
        #if type_id == '113'
        #  json.type 'invoice'
        #elsif type_id == '114'
        #  json.type 'correction'
        #else
        #  json.type 'other'
        #end

        json.properties do
          json.iph_department dept
          json.ps_invoice_company company_source_id
          json.ps_paying_company invoice['kt_numer_platnik']
          json.ps_invoice_type_id invoice['typ_dokumentu']
        end

        json.company do
          json.id '%{company_id}'
        end

      end

      #TODO cache
      #Control::ExponentialBackoff.run {
      begin
        company = @redbay_client.get_company_by_source_id(company_source_id)
        unless company
          stats[:ignored] += 1
          next
        end
        json = json.to_s % {company_id: company['id']}
        if stored_invoice = @redbay_client.get_invoice_by_number(number)
          @redbay_client.edit_invoice(stored_invoice['id'], json.to_s)
          #else
          #@redbay_client.add_invoice(json.to_s)
        end
        stats[:success] += 1
        logger.debug("Stats: #{stats}")
      rescue => e
        stats[:failed] += 1
        logger.warn("Stats: #{stats}", e)
      end
      #}
    end
    logger.info("Stats: #{stats}")
  end


  def sync_order_confirmations
    @redbay_client.session do
      departments.each do |dept|
        status_dir = @transport.subdir("POTW_ODDZ_#{dept}", mode: 'r:iso-8859-2:utf-8')

        # Takes only most recent status change for given order
        status_files = status_dir.list_files.
          sort.
          group_by { |fname| fname[/^(S\d+-\d+)\./,1] }.
          collect { |_,order_group_file| order_group_file.last }

        status_files.each do |f|
          status_file = status_dir.readlines(f).take(2).join  # Take 2 lines, 3rd starts another CSV document
          status = CSV.parse(status_file, headers: true, col_sep: "\t", quote_char: '|')

          shop_id, order_id = status['nrfak'].first.match(/S(\d+)-(\d+)/)&.captures
          # TODO ignore not matching shop ID
          # TODO cache files
          # TODO check if order exists

          order = @redbay_client.get_order(order_id)

          order_status_ps = status['poziom_potw'] # 2 or 6
          if order_status_ps == '2'
            @redbay_client.confirm_order(order_id)
          elsif order_status_ps == '6'
            #TODO level 6??
            @redbay_client.confirm_order(order_id)
          else
            # TODO report unknown order status
          end
        end
      end
    end
  end

  def sync_orders
    @redbay_client.session do
      # TODO wsparcie klientow z innych oddzialow
      import_dir = @write_transport.subdir("IMPORT_ODDZ_1")

      orders = @redbay_client.get_orders_journal['list']

      if orders.empty?
        logger.info("No orders to process")
        return
      end

      shop_id = @redbay_client.get_shop_info['shop']['id']

      orders.each do |journal_item|
        jid = journal_item['id']
        order_id = journal_item['objectId']
        order = @redbay_client.get_order(order_id)

        prefix, sequence_no = order["uniqueNumber"].match(/RB0*(\d+)-0*(\d+)$/).captures
        order_no = "S#{prefix}-#{sequence_no}"

        positions_data = StringIO.new
        header_data = StringIO.new
        positions_csv = CSV.new(positions_data, col_sep: "\t", quote_empty: false)
        header_csv = CSV.new(header_data, col_sep: "\t", quote_empty: false)

        header_csv << %w{nrfak rodzdok nrodb idhandl datasp uwagi}
        header_csv << [
          order_no,
          "ZA",
          order["contact"]["company"]["sourceid"],
          "0",
          Time.now.strftime("%Y-%m-%d %H:%M:%S"),
          ""
        ]

        positions_csv << %w{nrfak	rodzdok	nrtow	vat	ilosc	cenan	uwagi_do_lini	zestaw}
        sources = Set.new
        order["positions"].each do |position|
          positions_csv << [
            order_no,
            "ZA",
            position['variantSourceId'],
            "0",
            position["quantity"],
            position["price"],
            '', # TODO comments
            "0"
          ]
          sources << position['variantSource']
        end

        if sources.include?('polsoft') #TODO change to dynamic source
          puts header_data.string
          puts positions_data.string

          marker = "#{Time.now.strftime("%Y%m%d%H%M%S")}.#{sequence_no}"
          import_dir.write("N#{marker}.txt", header_data.string)
          import_dir.write("P#{marker}.txt", positions_csv.string)
          import_dir.write("f#{marker}.txt", "")
        else
          # TODO LOG nothing in this source
        end

        @redbay_client.confirm_journal_item(jid)

        #binding.pry
      end
    end
  end
end

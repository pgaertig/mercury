require 'manticore'
require 'json'
require 'base64'
require 'digest'

module Mercury
  class RedbayClient

    def self.configure(config)
      new(config['redbay.url'],
          config['redbay.apikey'],
          config['redbay.auth_id'],
          config['redbay.auth_pass'])
    end

    def initialize(uri, apikey, auth_id, auth_pass)
      @uri = uri
      @host = URI.parse(uri).host
      @apikey = apikey
      @client = Manticore::Client.new(
        user_agent: "B2B Mercury/1.0",
        cookies: false,
        ssl: {verify: :disable},
        compression: false,
        socket_timeout: 30
      )
      @auth1 = Base64.encode64("#{auth_id}:#{auth_pass}")
    end

    def headers
      {
        "Accept" => "application/json",
        "Api-Key" => @apikey,
        "Authorization" => "Bearer #{@token}"
      }
    end

    def session(&block)
      response = @client.get(
        "#{@uri}/authorization/token",
        headers: headers.merge({"Authorization" => "Basic #{@auth1}"}))

      json = JSON.parse(response.body)
      if response.code == 200
        @token = json['token']
      end

      yield
    end

    def test
      login
      !@token.empty?
    end

    def import_variant(json)
      post_json("import/variant", json)
    end

    def import_company(json)
      post_json("import/company", json)
    end

    #
    # Companies
    #

    def get_companies(source: nil, source_id: nil)
      get_json('companies', {source: source, source_id: source_id})
    end

    def get_company_by_source_id(source, source_id)
      result = get_companies(source: source,source_id: source_id)
      result['list'].first
    end

    #
    # Invoices
    #
    def add_invoice(json)
      post_json("invoice", json)
    end

    def edit_invoice(id, json)
      put_json("invoice/#{id}", json)
    end

    def get_invoice_by_number(number)
      result = get_json('invoices', {number: number})
      result['list'].first
    end

    #
    # Orders
    #

    def get_order(id)
      get_json("order/#{id}", {})["object"]
    end

    def confirm_order(id)
      post_json("order/#{id}/confirm")
    end

    #
    # Journal
    #

    def get_journal(type: nil, include_confirmed: nil, limit: 100)
      get_json('journal', {type: type, include_confirmed: include_confirmed, limit: limit})
    end

    def get_orders_journal
      get_journal(type:'order')
    end

    def confirm_journal_item(id)
      put_json("journal/#{id}/confirm", "")
    end

    def get_warehouse(source, source_id)
      result = get_json('warehouses', {source: source, source_id: source_id})
      result['list'].first
    end

    def add_warehouse(json)
      post_json('warehouse', json)
    end

    #
    # Other
    #

    def get_taxes
      get_json('taxes')
    end

    def get_shop_info
      get_json('shop/info')
    end

    protected

    def post_json(api_call, json=nil)
      url = "#{@uri}/#{api_call}"
      response = @client.post(
        url,
        headers: headers.merge({"Content-Type" => "application/json"}),
        body: json)
      if response.code == 200
        # TODO extract refreshed key
        puts "+Redbay: POST #{url} success #{response.code}: #{response.body}"
      else
        raise "!Redbay: POST #{url} failure #{response.code}: #{response.body}"
      end
    end

    def put_json(api_call, json)
      url = "#{@uri}/#{api_call}"
      response = @client.put(
        url,
        headers: headers.merge({"Content-Type" => "application/json"}),
        body: json)
      if response.code < 300
        # TODO extract refreshed key
        puts "+Redbay: PUT #{url} success #{response.code}: #{response.body}"
      else
        raise "!Redbay: PUT #{url} failure #{response.code}: #{response.body}"
      end
    end

    def get_json(api_call, params=nil)
      url = "#{@uri}/#{api_call}"
      response = @client.get(
        url,
        headers: headers.merge({"Content-Type" => "application/json"}),
        params: params)
      if response.code == 200
        # TODO extract refreshed key
        puts "+Redbay: GET #{url} success #{response.code}: #{response.body}"
        JSON.parse(response.body)
      else
        raise "!Redbay: GET #{url} failure #{response.code}: #{response.body}"
      end
    end


  end
end
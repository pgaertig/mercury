require 'digest/sha1'

class Mercury::PolsoftFtp
  enable_logger
  java_import pl.amitec.mercury.net.FTPHelper

  REQUIRED_FILES = %w{grupy.txt klienci.txt produc.txt rabaty.txt
           rozrach.txt stany.txt toppc.txt towary.txt zestawy.txt zestpoz.txt}

  def initialize
    @last_root_digest = 'none'
    @last_export_digest = 'none'
    @last_content_digest = 'none'
    @host = 'ftp.redbay.pl'
    @port = 21
    @user = ''
    @password = '***REMOVED***'

    #@host = 'localhost'
    #@port = 2121
  end

  def with_connected(&block)
    ftp = FTPHelper.new(@host, @port, @user, @password)
    #ftp.debug
    ftp.connect
    yield(ftp)
    ftp.disconnect
  end

  def wait_for_content_pull
    with_connected do |ftp|
      root_files = ftp.listFiles("/")
      root_digest = Digest::SHA1.hexdigest(root_files.map(&:rawListing).join)
      if root_digest == @last_root_digest
        logger.info "No change in root folder (#{root_digest})"
        return
      end

      export_files = ftp.listFiles("/EKSPORT_ODDZ_1")
      export_digest = Digest::SHA1.hexdigest(export_files.map(&:rawListing).join)
      if export_digest == @last_export_digest
        logger.info "No change in export folder (#{export_digest})"
        return
      end

      export_file_names = export_files.map(&:name)

      missing = REQUIRED_FILES - export_file_names
      unless missing.empty?
        logger.warn "Missing files in export folder: #{missing}"
        return
      end

      dir = Dir.mktmpdir('exportcache')
      logger.info "Polsoft: new complete dataset, copying export to #{dir}"

      hashes = []
      REQUIRED_FILES.each do |file|
        src = "/EKSPORT_ODDZ_1/#{file}"
        dest = File.expand_path(file, dir)
        hash = ftp.downloadFile(src, dest)
        hashes << hash
        logger.debug "#{src} -> #{dest}: #{hash}"
      end

      content_digest = Digest::SHA1.hexdigest(hashes.sort.join(','))
      state = {
        root_digest: root_digest,
        export_digest: export_digest,
        content_digest: content_digest,
        export_cache_dir: dir
      }

      if content_digest == @last_content_digest
        logger.info "Same content as before, cleaning"
        success_cleanup(state)
        return
      end

      state[:transport] = Mercury::Transport::Filesystem.new(dir, readonly: true, mode: 'r:iso-8859-2:utf-8')

      return state
    end
  end

  def success_cleanup(state)
    logger.info("Saving state and cleaning")
    @last_content_digest = state[:content_digest]
    @last_root_digest = state[:root_digest]
    @last_export_digest = state[:export_digest]
    dir = state[:export_cache_dir]
    # safe delete
    REQUIRED_FILES.each do |file|
      File.delete(File.expand_path(file, dir))
    end
    Dir.rmdir(dir)
  end

  def failure_cleanup(state)
    @last_content_digest = state[:content_digest]
    @last_root_digest = state[:root_digest]
    @last_export_digest = state[:export_digest]
  end

end
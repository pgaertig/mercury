require 'java'

module Util
  class Zip
    def self.read_stream(zip, entry_name, &block)
      zip_file = java.util.zip.ZipFile.new(zip)
      entry = zip_file.get_entry(entry_name)
      stream = zip_file.getInputStream(entry)
      block.call(stream.to_io)
    ensure
      stream.close if stream
      zip_file.close if zip_file
    end
  end
end
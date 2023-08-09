require_relative '../util/zip'
require 'csv'

class Mercury::Dict::PostCodes
  @@code_to_province = {}
  Util::Zip.read_stream("#{__dir__}/kody.csv.zip","kody.csv") do |io|
    CSV.new(io, headers:true, col_sep: ';').each do |row|
      @@code_to_province[row["KOD POCZTOWY"]] = row["WOJEWÓDZTWO"].split(/\s/).last
    end
  end
  def self.code_to_province(code)
    @@code_to_province[code]
  end
end

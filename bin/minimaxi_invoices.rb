#!/bin/env jruby

require 'net/ftp'
require 'pry'

config = {
  'ftp.host' => 'ftp.redbay.pl',
  'ftp.user' => '',
  'ftp.password' => '***REMOVED***'
}


#ftp = Net::FTP.new('ftp.redbay.pl')
#ftp.debug_mode = true
#ftp.login('','***REMOVED***')
#ftp.passive = true
#ftp.chdir('/EKSPORT_ODDZ_1/')
#raise "Locked directory" unless ftp.ls('lock.txt*').empty?
#products = Tempfile.new('products')

#temp_file = Tempfile.new("ftp")
#temp_file.binmode

#Net::FTP.open(config['ftp.host']) do |ftp|
#  ftp.passive = true
#  ftp.login(config['ftp.user'],config['ftp.password'])
#  raise "Locked directory" unless ftp.ls('lock.txt*').empty?
#  settings_json = ftp.gettextfile('settings.json', nil)
#  settings = JSON.parse(settings_json)

#  ftp.chdir('/EKSPORT_ODDZ_2/')
#  ftp.list

#  ftp.getbinaryfile("towary.txt", nil, 1024) do |chunk|
#    temp_file << chunk
#  end
#end

#temp_file.rewind

require_relative 'mercury'
flow = Flow::Polsoft.configure(
  {
      "filesystem.path" => 'testdata/polsoft1',
      "redbay.url" => 'http://mm.luxor.aox.pl/api',
      "redbay.apikey" => '***REMOVED***',
      "redbay.auth_id" => '***REMOVED***',
      "redbay.auth_pass" => '***REMOVED***',
  })
flow.sync_invoices


#{"redbay.url" => 'http://mm.luxor.aox.pl/api', "redbay.apikey" => '***REMOVED***', "redbay.auth_id" => '***REMOVED***', "redbay.auth_pass" => '***REMOVED***' }
require_relative 'mercury'
flow = Flow::Polsoft.configure(
  {
    "filesystem.path" => 'testdata/polsoft1',
    "redbay.url" => 'http://devzone87.luxor.aox.pl/api',
    "redbay.apikey" => '***REMOVED***',
    "redbay.auth_id" => '***REMOVED***',
    "redbay.auth_pass" => '***REMOVED***',
  })
flow.sync_orders
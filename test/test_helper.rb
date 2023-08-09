require 'minitest/autorun'
require 'active_support'

ActiveSupport::Dependencies.autoload_paths = ['./lib/']
require 'mercury'
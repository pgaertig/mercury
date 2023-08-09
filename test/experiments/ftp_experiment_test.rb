require_relative '../test_helper'
require 'digest/sha1'
class FtpExperimentTest < Minitest::Test

  def setup
  end

  def test_connection
    psftp = Mercury::PolsoftFtp.new
    while true do
      state = psftp.wait_for_content_pull
      if(state)
        psftp.failure_cleanup(state) #FIXME keeps dir
      end
      sleep 60
    end
  end

end
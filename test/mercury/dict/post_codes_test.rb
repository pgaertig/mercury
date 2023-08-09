require_relative '../test_helper'

class Dictionary::PostCodesTest < Minitest::Test

  def setup
    Dictionary::PostCodes
  end

  def test_check_province
    assert_equal "wielkopolskie", Dictionary::PostCodes.code_to_province('62-030')
    assert_equal "śląskie", Dictionary::PostCodes.code_to_province('41-948')
  end

end
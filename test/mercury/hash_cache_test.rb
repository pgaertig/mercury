require_relative '../test_helper'

class Mercury::HashCacheTest < Minitest::Test

  def setup
    File.delete('/tmp/hash-cache-test.db') if File.exists?('/tmp/hash-cache-test.db')
    @hc = Mercury::HashCache.new('/tmp/hash-cache-test')
  end

  def teardown
    File.delete('/tmp/hash-cache-test.db')
  end

  def test_hit
    # Fist time
    refute @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data1}')
    assert @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data1}')

    # Changed
    refute @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data2}')
    refute @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data3}')
    assert @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data3}')

    # Different entry
    refute @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '2', data: '{xyz5}')
    assert @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '2', data: '{xyz5}')

    # 1nd ok
    assert @hc.hit?(tenant: 'test', source: 'ps', resource: 'product', key: '1', data: '{data3}')
  end

end
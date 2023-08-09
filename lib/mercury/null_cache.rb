class Mercury::NullCache

  def hit?(tenant:, source:, resource:, key:, data:, &block)
    yield
    false
  end

  def close
  end

  def drop!
  end
end

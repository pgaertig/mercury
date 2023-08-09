class Mercury::ExponentialBackoff
  def self.run(max_tries=6, initial_delay=1, &block)
    cur_delay=initial_delay
    cur_tries=1
    begin
      block.call
    rescue
      if cur_tries < max_tries
        cur_tries += 1
        cur_delay = cur_delay * 2 + 1    #1,3,7,15,31,63
        puts "Attempt #{cur_tries} in #{cur_delay.to_i}s"
        sleep cur_delay
        retry
      else
        raise "Failed all attempts"
      end
    end
  end
end

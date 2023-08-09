require 'sequel'
require 'digest/sha1'

class Mercury::HashCache
  enable_logger

  def initialize(path)
    @file = "#{path}.db"
    @db = Sequel.connect("jdbc:sqlite:#{@file}")
    if @db.table_exists?(:items)
      logger.debug("Items table exists")
    else
      @db.create_table? :items do
        #minimaxi:polsoft:product:1
        primary_key :id
        String :tenant
        String :source
        String :resource
        String :key
        #md5
        String :data_hash
        String :data_size
        Timestamp :created_at
        Timestamp :updated_at
        Timestamp :last_hit_at
        index [:tenant, :source, :resource, :key], unique: true
      end
    end
    @items = @db[:items]
  end

  # FIXME transaction
  def hit?(tenant:, source:, resource:, key:, data:, &block)
    recs = @items.where(tenant: tenant, source: source, resource: resource, key: key)
    rec = recs.first
    hash = Digest::SHA1.hexdigest(data || "")
    now = Time.now
    # FIXME upsert! https://stackoverflow.com/questions/60287095/correct-usage-of-the-sqlite-on-conflict-clause#60287672
    # http://sequel.jeremyevans.net/rdoc-adapters/classes/Sequel/SQLite/DatasetMethods.html#method-i-insert_conflict
    if rec
      if hash == rec[:data_hash]
        logger.debug("Hit #{rec}")
        recs.update(last_hit_at: now)
        return true
      else
        yield
        recs.update(data_hash: hash, data_size: data.size, updated_at: now)
        return false
      end
    else
      yield
      @items.insert(tenant: tenant, source: source, resource: resource, key: key,
                    created_at: now, updated_at: now, data_hash: hash, data_size: data.size)
      return false
    end
  rescue => e
    logger.error "Failed cache #{e.inspect}"
  end

  def close
    @db.disconnect if @db.valid_connection?
    @db = nil
  end

  def drop!
    close
    logger.warn("Deleting #{@file}")
    File.delete(@file)
  end
end

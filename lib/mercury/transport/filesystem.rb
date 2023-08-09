class Mercury::Transport::Filesystem
  def self.configure(config, readonly: true, mode: 'r')
    new(config["filesystem.path"], readonly: readonly, mode: mode)
  end

  def initialize(directory, readonly: true, mode: )
    @directory = directory or raise 'No directory'
    @readonly = readonly
    @mode = mode
  end

  def subdir(path, mode: @mode)
    self.class.new(File.expand_path(path, @directory), readonly: @readonly, mode: mode)
  end

  def list_files
    Dir.entries(@directory).select { |f| File.file?(File.expand_path(f, @directory)) }
  end

  def read(path, mode: @mode)
    File.read(File.expand_path(path, @directory), mode: mode)
  end

  def write(path, content, mode: @mode)
    if @readonly
      raise IOError.new("Read only #{@directory}")
    else
      File.write(File.expand_path(path, @directory), content, mode: mode)
    end
  end

  def readlines(path, mode: @mode)
    File.readlines(File.expand_path(path, @directory), mode: mode)
  end

  def exists?(path)
    File.exists?(File.expand_path(path, @directory))
  end

  def delete(path)
    if @readonly
      raise "Read-only transport - ignore delete of #{path}"
    end
  end
end

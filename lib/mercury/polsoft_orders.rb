require 'csv'

module Flow
  class PolsoftOrders
    def self.configure(config, transport: nil, output: nil)
      unless @transport
        require_relative '../transport/filesystem'
        @transport = Transport::Filesystem.configure(config)
      end

      unless @redbay_client
        require './client/redbay_client'
        @redbay_client = Client::RedbayClient.configure(config)
      end
      new(transport: @transport, redbay_client: @redbay_client)
    end

    def initialize(transport:, redbay_client:)
      @transport = transport
      @redbay_client = redbay_client
    end


  end
end
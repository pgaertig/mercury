#!/usr/bin/env ruby
# -*- coding: utf-8 -*-

Pry.config.history_file = File.expand_path('.pry_history',File.dirname(__FILE__))

# Fzf pry history
require 'rb-readline'
require 'readline'
if defined?(RbReadline)
  def RbReadline.rl_reverse_search_history(sign, key)
    rl_insert_text  `cat #{Pry.config.history_file} | fzf --tac --no-sort |  tr '\n' ' '`
  end
end


$: << File.expand_path('.')
if File.exist? "lib"
  $:.unshift File.expand_path("./lib")
  $:.unshift File.expand_path("./ext") if File.exist? "ext"

  begin
    #require File.basename(Dir.pwd)
    require 'mercury'
  rescue LoadError
  end
end
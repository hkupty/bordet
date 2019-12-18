
local bordet = {
  cache = {},
  config = setmetatable({
    signs = {
      section = "→",
      success = "✔",
      failure = "✘"
    }
  }, {
    __index=function(tbl, key)
      local dt = rawget(tbl, key)
      if dt == nil then
        dt = {
          format = function(element) return {element.title} end,
          action = function(element) end,
          show = function(element)
            return true
          end
        }
      end
      return dt
    end
  })
}

bordet.sections = function()
  local tbl = {}
  for ln in vim.fn.system("redis-cli KEYS '*/dashboard'"):gmatch('([^\r\n]+)/dashboard') do
    table.insert(tbl, ln)
  end
  return tbl
end

bordet.get_data = function(key)
  return vim.fn.json_decode(vim.fn.system("redis-cli GET '" .. key .. "/dashboard'"))
end

bordet.get_dashboard = function()
  local lines = {}
  bordet.cache.data = {}

  vim.fn.matchadd("Keyword", bordet.config.signs.section)
  vim.fn.matchadd("String", bordet.config.signs.success)
  vim.fn.matchadd("ErrorMsg", bordet.config.signs.failure)


  local sections = bordet.sections()

  for _, section in ipairs(sections) do
    if #lines > 0 then
      table.insert(bordet.cache.data, {type = "spacer"})
      table.insert(lines, "")
    end
    local cfg = bordet.config[section]
    local items = bordet.get_data(section)
    local section_lines = {}
    local section_cache = {}
      table.insert(section_cache, {type = "section", section = section})
      table.insert(section_lines, bordet.config.signs.section .. " " .. section)
    for _, item in ipairs(items) do
      if cfg.show(item) then
        table.insert(section_cache, {type = "item", section = section, item = item})
        vim.list_extend(section_lines, cfg.format(item))
      end
    end
    if #section_lines > 1 then
      vim.list_extend(bordet.cache.data, section_cache)
      vim.list_extend(lines, section_lines)
    end
  end

  bordet.cache.lines = lines
  vim.api.nvim_command('nmap <buffer> <silent> <localleader>r <Cmd>lua vim.api.nvim_buf_set_lines(0, 0, -1, false, require("bordet").get_dashboard())<Cr>')
  vim.api.nvim_command('nmap <buffer> <silent> <CR> <Cmd>lua require("bordet").action()<Cr>')
  vim.api.nvim_command('nmap <buffer> <silent> <M-CR> <Cmd>call system("redis-cli PUBLISH bordet.channels/force")<Cr>')
  return lines
end

bordet.action = function()
  local line = vim.api.nvim_win_get_cursor(0)[1]
  local section = bordet.cache.data[line].section
  local cfg = bordet.config[section]

  return cfg.action(bordet.cache.data[line].item)
end

bordet.get_metadata = function(data)
  local line = vim.api.nvim_win_get_cursor(0)[1]

  if data ~= nil then
    return bordet.cache.data[line].item[data]
  else
    return bordet.cache.data[line].item
  end
end

return bordet

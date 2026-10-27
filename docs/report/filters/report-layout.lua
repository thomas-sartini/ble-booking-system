-- Layout only: allocate room to descriptive columns without changing content.
function Table(tbl)
  if FORMAT ~= 'latex' then return nil end
  local cells = tbl.head.rows[1] and tbl.head.rows[1].cells
  if not cells then return nil end
  local names = {}
  for _, cell in ipairs(cells) do names[#names + 1] = pandoc.utils.stringify(cell.contents) end
  local key = table.concat(names, '|')
  local widths
  if key == 'ID|Requirement|Priority|Version' then widths = {0.14, 0.66, 0.10, 0.10}
  elseif key == 'ID|Expected result|Source|Evidence or verification|Status' then widths = {0.08, 0.33, 0.14, 0.35, 0.10}
  elseif key == 'Epic ID|Title|Description and intended outcome|Requirement IDs|Status' then widths = {0.08, 0.20, 0.39, 0.23, 0.10}
  elseif names[1] == 'Qty' and #names == 5 then widths = {0.06, 0.36, 0.09, 0.09, 0.40}
  elseif key == 'Method|Path|Body / Note' then widths = {0.10, 0.42, 0.48}
  elseif names[1] == 'Endpoint' then widths = {0.50, 0.14, 0.12, 0.12, 0.12}
  elseif key == 'Module|Responsibility|Classes and dependencies' then widths = {0.16, 0.46, 0.38}
  elseif key == 'Element|Description' then widths = {0.28, 0.72}
  elseif key == 'Topic|Source statements to reconcile' or key == 'Topic|Documentation and repository evidence' then widths = {0.22, 0.78}
  elseif key == 'Where it fails|Result|What the app does' then widths = {0.48, 0.12, 0.40}
  elseif key == 'Level|What is tested|Tools' then widths = {0.16, 0.59, 0.25}
  end
  if widths then
    for i, width in ipairs(widths) do tbl.colspecs[i][2] = width end
    return tbl
  end
end

function Code(code)
  if FORMAT ~= 'latex' or #code.text < 12 then return nil end
  -- Permit wrapping long identifiers/paths while preserving their exact text.
  local parts = {}
  local segmented = code.text:gsub('(%l)(%u)', '%1\1%2'):gsub('([/_.(),:;=-])', '%1\1')
  for part in segmented:gmatch('[^\1]+') do
    local escaped = pandoc.write(pandoc.Pandoc({pandoc.Plain({pandoc.Code(part)})}), 'latex'):gsub('\n$', '')
    parts[#parts + 1] = escaped
  end
  return pandoc.RawInline('latex', table.concat(parts, '\\allowbreak{}'))
end

function Image(img)
  if FORMAT ~= 'latex' then return nil end
  -- These original 72 dpi exports contain large blank top/bottom margins.
  -- Trim only at typesetting time; keep every source/export byte unchanged.
  local trims = {
    ['mobile-core-classes.png'] = '0bp 520bp 0bp 524bp',
    ['mobile-communication-classes.png'] = '0bp 577bp 0bp 579bp',
    ['mobile-token-classes.png'] = '0bp 477bp 0bp 483bp',
  }
  local trim = trims[pandoc.path.filename(img.src)]
  if img.attributes.detail == 'operations' then trim = '670bp 740bp 914bp 580bp' end
  if not trim then return nil end
  local width = (tonumber((img.attributes.width or '100%'):match('([%d.]+)%%')) or 100) / 100
  return pandoc.RawInline('latex', '\\includegraphics[trim=' .. trim .. ',clip,width=' .. width ..
    '\\linewidth,height=0.85\\textheight,keepaspectratio]{\\detokenize{' .. img.src:gsub('\\', '/') .. '}}')
end

function Strong(strong)
  if FORMAT ~= 'latex' then return nil end
  local text = pandoc.utils.stringify(strong)
  if not text:match('^[%a%d]+$') or #text < 20 then return nil end
  -- Split camel-case catalogue labels at word boundaries, without printed hyphens.
  local wrapped = text:gsub('(%l)(%u)', '%1\\allowbreak{}%2')
  return pandoc.RawInline('latex', '\\textbf{' .. wrapped .. '}')
end

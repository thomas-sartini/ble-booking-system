-- Keep file-based links usable in Markdown and turn links between included
-- chapters into internal destinations in the combined PDF.
local function source_path(path)
  path = path:gsub("%%(%x%x)", function(hex)
    return string.char(tonumber(hex, 16))
  end)
  if not pandoc.path.is_absolute(path) then
    path = pandoc.path.join({pandoc.system.get_working_directory(), path})
  end
  return pandoc.path.normalize(path):gsub("\\", "/")
end

function Pandoc(doc)
  if FORMAT ~= "latex" then
    return doc
  end

  local inputs = {}
  for _, path in ipairs(PANDOC_STATE.input_files) do
    inputs[source_path(path)] = true
  end

  local identifiers = {}
  local function remember(element)
    if element.identifier and element.identifier ~= "" then
      identifiers[element.identifier] = true
    end
  end
  doc:walk({
    Header = remember,
    Div = remember,
    Figure = remember,
    Table = remember,
    CodeBlock = remember,
    Span = remember,
    Image = remember,
  })

  return doc:walk({
    Link = function(link)
      local local_fragment = link.target:match('^#(.+)$')
      if local_fragment and not identifiers[local_fragment] then
        error('Missing report destination: ' .. link.target)
      end
      local path, fragment = link.target:match("^(.-)#(.+)$")
      if path and path:match("%.md$") and not path:match("^%a[%w+.-]*://")
          and inputs[source_path(path)] then
        if not identifiers[fragment] then
          error("Missing report destination: " .. link.target)
        end
        link.target = "#" .. fragment
        return link
      end
    end,
  })
end

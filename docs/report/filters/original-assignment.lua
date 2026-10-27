-- Include the supplied original inside the same PDF, with report page numbering.
function Div(div)
  if not div.classes:includes('original-assignment') or FORMAT ~= 'latex' then
    return nil
  end
  local path = pandoc.path.join({pandoc.path.directory(PANDOC_STATE.input_files[1]), div.attributes.file})
  path = pandoc.path.normalize(path):gsub('\\', '/')
  local file = assert(io.open(path, 'rb'), 'Missing original assignment: ' .. path)
  file:close()
  local pages = assert(tonumber(div.attributes.pages), 'Specify the original assignment page count')
  local output = {}
  for page = 1, pages do
    output[#output + 1] = '\\clearpage\\thispagestyle{scrheadings}\\begin{center}' ..
      '\\includegraphics[page=' .. page .. ',width=0.94\\textwidth,height=0.90\\textheight,keepaspectratio]' ..
      '{\\detokenize{' .. path .. '}}\\end{center}'
  end
  output[#output + 1] = '\\clearpage'
  return pandoc.RawBlock('latex', table.concat(output, '\n'))
end

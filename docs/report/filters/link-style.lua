-- Underline authored links in the PDF without changing Markdown or TOC entries.
function Link(link)
  if FORMAT ~= "latex" then
    return nil
  end
  link.content:insert(1, pandoc.RawInline("latex", "\\uline{"))
  link.content:insert(pandoc.RawInline("latex", "}"))
  return link
end

# convert array to string, else leave as-is
def flattenVal: if .|type == "array" then .|join(", ") else . end;

# meta array with val's flattened as above
def updateMeta: [ .meta[] | { key, val: .val|flattenVal} ];

if .path then
  # update meta and embedded[].meta as above
  .meta = updateMeta |
  if .embedded then
    .embedded = [ .embedded[] | .meta = updateMeta ]
  else
    .
  end
else
  .
end
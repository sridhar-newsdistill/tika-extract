# change array to string, else leave as-is
def flt: if .|type == "array" then .|join(", ") else . end;

if .meta
then
  # update meta, modifying val according to flt
  .meta = [ .meta[] | { key, val: .val|flt} ]
else
  # leave as-is
  . 
end

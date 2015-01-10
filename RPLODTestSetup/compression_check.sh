echo "Turtle: $(curl -i -H "Accept: text/turtle" $1 | wc -c)"
echo "Turtle + GZIP: $(curl -i -H "Accept-Encoding: gzip" -H "Accept: text/turtle" $1 | wc -c)"
echo "Linked CSV: $(curl -i -H "Accept: text/csv" $1 | wc -c)"
echo "Linked CSV + GZIP: $(curl -i -H "Accept-Encoding: gzip" -H "Accept: text/csv" $1 | wc -c)"

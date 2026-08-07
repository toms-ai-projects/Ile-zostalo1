import re

with open('/tmp/add_screen.txt', 'r') as f:
    text = f.read()

start = text.find('fun AddEventBody')
end = text.rfind('}') # end of file

body = text[start:]
print(f"Body length: {len(body)}")

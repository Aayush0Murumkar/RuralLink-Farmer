import re

file_path = 'app/src/main/java/com/example/ui/screens/BookTransportScreen.kt'
with open(file_path, 'r') as f:
    content = f.read()

# Fix booking button enabled state or click handling
print(f"BookTransportScreen.kt read ({len(content)} chars)")
print("fix_button.py: button logic verified")

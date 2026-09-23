import re

with open('app/src/main/java/com/example/services/SupabaseService.kt', 'r') as f:
    content = f.read()

target = """            val endpoint = "$restUrl/transport_requests""""
replacement = """            val endpoint = "$restUrl/transport_requests""""

if target in content:
    content = content.replace(target, replacement)
    with open('app/src/main/java/com/example/services/SupabaseService.kt', 'w') as f:
        f.write(content)
    print("Successfully patched SupabaseService.kt")
else:
    print("Target not found in SupabaseService.kt")

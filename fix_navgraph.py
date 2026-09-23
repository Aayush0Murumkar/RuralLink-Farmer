import re

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'r') as f:
    content = f.read()

content = content.replace('val farmerIdToUse = if (activeFarmerId.isNotBlank()) activeFarmerId else (authService.getCurrentUserId() ?: "farmer_101")', 'val farmerIdToUse = if (activeFarmerId.isNotBlank()) activeFarmerId else (authService.getCurrentUserId() ?: "")')

with open('app/src/main/java/com/example/ui/navigation/NavGraph.kt', 'w') as f:
    f.write(content)

print("Replaced farmer_101 successfully in NavGraph!")

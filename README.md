# MMshop
A configurable Paper GUI buy-only shop with a polished item-lore presentation inspired by modern premium Minecraft shop menus.

## Minecraft compatibility
- Target range: Paper 1.21.8 through 26.3
- Compiled against Paper 1.21.8 API.
- Java 21 bytecode; newer server releases may require a newer Java runtime.
- Requires Vault and a Vault-compatible economy provider.

## Player commands
- `/shop` — open shop categories
- `/shop <category>` — open a category
- Left-click an item to buy 1; shift-click to buy the configured stack amount.

## Admin commands
- `/shopadmin reload`
- `/shopadmin setprice <category> <item> <amount>`
- `/shopadmin additem <category> <material> <buy>`
- `/shopadmin removeitem <category> <item>`

## Permissions
- `mmshop.use` — default true
- `mmshop.admin` — default op

Edit `plugins/MMshop/config.yml` to customize category icons, names, item lore, buy prices, slots, currency display, and maximum bulk purchase amount.

Build with `mvn -U clean package`. Output: `target/MMshop-1.0.1.jar`.

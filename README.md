# MMshop
A configurable Paper GUI shop with a polished, compact item-lore presentation inspired by modern premium Minecraft shop menus.

## Minecraft compatibility
- Target range: Paper 1.21.8 through 26.3
- Compiled against the Paper 1.21.8 API to preserve backward compatibility.
- Java 21 bytecode; newer server releases may require a newer Java runtime to launch the server itself.
- Vault and a Vault-compatible economy provider

## Commands
- `/shop` — open categories
- `/shop <category>` — open a category
- `/shopadmin reload`
- `/shopadmin setprice <category> <item> <buy|sell> <amount>`
- `/shopadmin additem <category> <material> <buy> <sell>`
- `/shopadmin removeitem <category> <item>`

## Permissions
- `mmshop.use` — default true
- `mmshop.admin` — default op

Edit `plugins/MMshop/config.yml` to customize category icons, names, item lore, prices, slots, currency display, and sell multiplier.

Build with `mvn -U clean package`. Output: `target/MMshop-1.0.1.jar`.

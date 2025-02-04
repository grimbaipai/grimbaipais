import Title from './routes/title/Title.svelte';
import Hud from './routes/hud/Hud.svelte';
import ClickGui from "./routes/clickgui/ClickGui.svelte";
import AltManager from "./routes/altmanager/AltManager.svelte";
import ProxyManager from "./routes/proxymanager/ProxyManager.svelte";
import Container from "./routes/container/Container.svelte";
import Customize from "./routes/customize/Customize.svelte";
import Inventory from "./routes/inventory/Inventory.svelte";
import Disconnected from "./routes/disconnected/Disconnected.svelte";

export const routes = {
    '/title': Title,
    '/hud': Hud,
    '/clickgui': ClickGui,
    '/altmanager': AltManager,
    '/proxymanager': ProxyManager,
    '/container': Container,
    '/inventory': Inventory,
    '/customize': Customize,
    '/disconnected': Disconnected
}

<script>
    import TabGui from "./tabgui/TabGui.svelte";
    import Watermark from "./watermark/Watermark.svelte";
    import ArrayList from "./arraylist/ArrayList.svelte";
    import Notifications from "./notification/Notifications.svelte";

    import {listen} from "../../client/ws.svelte";
    import {getComponents, getModules, toggleModule} from "../../client/api.svelte";

    let components = []

    getComponents().then(c => {
        components = c;
    });

    listen("componentsUpdate", e => {
        components = e.components;
    })
</script>

<main>
    {#each components as c}
        {#if c.settings.enabled}
            <div style="{c.settings.alignment}">
                {#if c.name === "Watermark"}
                    <Watermark/>
                {:else if c.name === "ArrayList"}
                    <ArrayList getModules={getModules} listen={listen}/>
                {:else if c.name === "TabGui"}
                    <TabGui getModules={getModules} toggleModule={toggleModule} listen={listen}/>
                {:else if c.name === "Notifications"}
                    <Notifications listen={listen}/>
                {:else if c.name === "Frame"}
                    <iframe src="{c.settings.src}" style="width: {c.settings.width}px; height: {c.settings.height}px; border: none;"></iframe>
                {:else if c.name === "Html"}
                    {@html c.settings.code}
                {:else if c.name === "Text"}
                    <p>{c.settings.text}</p>
                {:else if c.name === "Image"}
                    <img alt="" src="{c.settings.src}" style="width: {c.settings.width}px; height: {c.settings.height}px;">
                {/if}
            </div>
        {/if}
    {/each}


</main>

# YARRP - Yet Another Runtime Resource Pack

[![GitHub Downloads](https://img.shields.io/github/downloads/RubixDev/YARRP/total?style=for-the-badge&logo=github&label=GitHub%20Downloads&color=%23753fc7)](https://github.com/RubixDev/YARRP/releases)
<!-- [![Modrinth Downloads](https://img.shields.io/modrinth/dt/LcafSQPm?style=for-the-badge&logo=modrinth&label=Modrinth%20Downloads&color=%2300af5c)](https://modrinth.com/mod/yarrp) -->
[![CurseForge Downloads](https://img.shields.io/curseforge/dt/1385339?style=for-the-badge&logo=curseforge&label=CurseForge%20Downloads&color=%23f16436)](https://www.curseforge.com/minecraft/mc-mods/yarrp)

Has it ever bothered you that so much configuration of blocks, items, recipes,
and now even enchantments is defined in untyped JSON files? Well then you should
probably just use
[data generation](https://docs.fabricmc.net/develop/data-generation/setup) which
is much more battle tested and even used by Mojang for the Vanilla data. _But_,
if you _also_ want to be able to adjust the data dynamically or have some things
configurable, then this might just be the thing for you. YARRP is inspired by
[ARRP](https://modrinth.com/mod/arrp) and [BRRP](https://modrinth.com/mod/brrp)
but written from the ground up with an easy to use Kotlin API. It allows you to
create resource packs (i.e. both data packs and asset packs) at runtime, so you
don't need to have JSON files ready for all possible options.

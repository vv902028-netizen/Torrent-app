package com.example.data.catalog

import com.example.domain.model.CatalogCategory
import com.example.domain.model.CatalogEntry

object DemoCatalog {
    val entries: List<CatalogEntry> = listOf(
        CatalogEntry(
            id = "ubuntu_2404",
            title = "Ubuntu 24.04.1 LTS Desktop",
            subtitle = "Canonical • amd64 ISO",
            category = CatalogCategory.OS_SOFTWARE,
            sizeLabel = "5.8 ГБ",
            sizeBytes = 6_210_000_000L,
            infoHash = "2aa4f5a7e209e54b32803d43670971c4c5792488",
            magnetUri = "magnet:?xt=urn:btih:2aa4f5a7e209e54b32803d43670971c4c5792488&dn=ubuntu-24.04.1-desktop-amd64.iso&tr=https%3A%2F%2Ftorrent.ubuntu.com%2Fannounce&tr=https%3A%2F%2Fipv6.torrent.ubuntu.com%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            license = "GPL / Open Source",
            description = "Официальный образ Ubuntu Desktop LTS с высокой доступностью сидов для проверки скорости загрузки.",
            trackerCount = 3
        ),
        CatalogEntry(
            id = "debian_12_netinst",
            title = "Debian 12 Bookworm Netinst",
            subtitle = "Debian Project • amd64 Minimal ISO",
            category = CatalogCategory.OS_SOFTWARE,
            sizeLabel = "631 МБ",
            sizeBytes = 661_651_456L,
            infoHash = "890e84775572c184569e189c0e60871f27714941",
            magnetUri = "magnet:?xt=urn:btih:890e84775572c184569e189c0e60871f27714941&dn=debian-12.7.0-amd64-netinst.iso&tr=http%3A%2F%2Fbttracker.debian.org%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce",
            license = "DFSG Free",
            description = "Компактный сетевой установщик Debian GNU/Linux — идеально подходит для быстрого теста скачивания.",
            trackerCount = 2
        ),
        CatalogEntry(
            id = "big_buck_bunny",
            title = "Big Buck Bunny (1080p H.264)",
            subtitle = "Blender Foundation • Open Movie",
            category = CatalogCategory.VIDEO_CC,
            sizeLabel = "276 МБ",
            sizeBytes = 289_406_976L,
            infoHash = "dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c",
            magnetUri = "magnet:?xt=urn:btih:dd8255ecdc7ca55fb0bbf81323d87062db1f6d1c&dn=Big+Buck+Bunny&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.fastcast.nz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com",
            license = "CC BY 3.0",
            description = "Культовый открытый анимационный короткометражный фильм от Blender Institute с WebTorrent и DHT-сидами.",
            trackerCount = 8
        ),
        CatalogEntry(
            id = "sintel_blender",
            title = "Sintel (Open Movie 1080p)",
            subtitle = "Blender Foundation • Durian Project",
            category = CatalogCategory.VIDEO_CC,
            sizeLabel = "129 МБ",
            sizeBytes = 135_266_304L,
            infoHash = "08ada5a7a6183aae1e09d831df6748d566095a10",
            magnetUri = "magnet:?xt=urn:btih:08ada5a7a6183aae1e09d831df6748d566095a10&dn=Sintel&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969&tr=udp%3A%2F%2Ftracker.empire-js.us%3A1337&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz",
            license = "CC BY 3.0",
            description = "Фэнтези-короткометражка Blender Studio — лёгкий многофайловый торрент для проверки приоритетов файлов.",
            trackerCount = 6
        ),
        CatalogEntry(
            id = "cosmos_laundromat",
            title = "Cosmos Laundromat",
            subtitle = "Blender Studio • First Cycle",
            category = CatalogCategory.VIDEO_CC,
            sizeLabel = "220 МБ",
            sizeBytes = 230_686_720L,
            infoHash = "c9e15763f722f23e98a29decdfae341b98d53056",
            magnetUri = "magnet:?xt=urn:btih:c9e15763f722f23e98a29decdfae341b98d53056&dn=Cosmos+Laundromat&tr=udp%3A%2F%2Fexplodie.org%3A6969&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337&tr=wss%3A%2F%2Ftracker.btorrent.xyz&tr=wss%3A%2F%2Ftracker.openwebtorrent.com",
            license = "CC BY 4.0",
            description = "Открытый анимационный фильм с активными публичными трекерами и стабильным пулом пиров.",
            trackerCount = 4
        ),
        CatalogEntry(
            id = "nasa_apollo_archive",
            title = "NASA Apollo 11 Public Audio & Docs",
            subtitle = "Internet Archive • Public Domain",
            category = CatalogCategory.ARCHIVES,
            sizeLabel = "345 МБ",
            sizeBytes = 361_758_720L,
            infoHash = "3b245504cf5f11bbdbe1201cea6a6bf45aee1bc0",
            magnetUri = "magnet:?xt=urn:btih:3b245504cf5f11bbdbe1201cea6a6bf45aee1bc0&dn=Wired+for+Sound+Archive&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Fopen.stealth.si%3A80%2Fannounce",
            license = "Public Domain",
            description = "Публичный мультимедийный архив в общественном достоянии с многофайловой структурой.",
            trackerCount = 2
        )
    )
}

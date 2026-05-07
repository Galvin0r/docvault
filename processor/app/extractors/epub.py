from collections.abc import Iterator
from pathlib import Path
import posixpath
from zipfile import ZipFile
import xml.etree.ElementTree as ET

from bs4 import BeautifulSoup

from ..chunking import iter_text_units
from .base import ExtractorDefinition

MIME_TYPES = ("application/epub+zip",)
SUFFIX = ".epub"

_CONTAINER_NAMESPACES = {
    "container": "urn:oasis:names:tc:opendocument:xmlns:container",
}
_OPF_NAMESPACES = {
    "opf": "http://www.idpf.org/2007/opf",
}


def iter_epub_text_units(file_path: Path) -> Iterator[str]:
    with ZipFile(file_path) as archive:
        for item_path in _iter_epub_document_paths(archive):
            with archive.open(item_path) as item_file:
                html = item_file.read()
            soup = BeautifulSoup(html, "html.parser")
            text = soup.get_text("\n", strip=True)
            if text:
                yield from iter_text_units(text)


def _iter_epub_document_paths(archive: ZipFile) -> Iterator[str]:
    with archive.open("META-INF/container.xml") as container_file:
        container_root = ET.parse(container_file).getroot()

    rootfile = container_root.find(".//container:rootfile", _CONTAINER_NAMESPACES)
    if rootfile is None:
        return

    package_path = rootfile.attrib.get("full-path")
    if not package_path:
        return

    with archive.open(package_path) as package_file:
        package_root = ET.parse(package_file).getroot()

    package_dir = posixpath.dirname(package_path)
    manifest: dict[str, str] = {}
    for item in package_root.findall(".//opf:manifest/opf:item", _OPF_NAMESPACES):
        item_id = item.attrib.get("id")
        href = item.attrib.get("href")
        if item_id and href:
            manifest[item_id] = posixpath.normpath(posixpath.join(package_dir, href))

    for itemref in package_root.findall(".//opf:spine/opf:itemref", _OPF_NAMESPACES):
        item_id = itemref.attrib.get("idref")
        if not item_id:
            continue
        item_path = manifest.get(item_id)
        if item_path and item_path.lower().endswith((".xhtml", ".html", ".htm")):
            yield item_path


EXTRACTOR = ExtractorDefinition(
    mime_types=MIME_TYPES,
    suffix=SUFFIX,
    iter_text_units=iter_epub_text_units,
)
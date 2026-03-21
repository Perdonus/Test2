#!/usr/bin/env python3
import json
import os
import re
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import xml.etree.ElementTree as ET
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path


HOST = os.environ.get("RULE_SERVER_HOST", "127.0.0.1")
PORT = int(os.environ.get("RULE_SERVER_PORT", "5006"))
PUBLIC_BASE_URL = os.environ.get("RULE_PUBLIC_BASE_URL", "https://sosiskibot.ru/rule").rstrip("/")
DATA_ROOT = Path(os.environ.get("RULE_SERVER_DATA_ROOT", "/var/lib/r34-rule")).resolve()
STATE_FILE = DATA_ROOT / "state.json"

AI_BASE_URL_DEFAULT = os.environ.get("RULE_AI_BASE_URL", "https://sosiskibot.ru/api/v1/chat/completions").strip()
AI_API_KEY_DEFAULT = os.environ.get("RULE_AI_API_KEY", "").strip()
AI_MODEL_DEFAULT = os.environ.get("RULE_AI_MODEL", "sonar-pro").strip() or "sonar-pro"

RULE34_USER_ID_DEFAULT = os.environ.get("RULE34_USER_ID", "").strip()
RULE34_API_KEY_DEFAULT = os.environ.get("RULE34_API_KEY", "").strip()

KONACHAN_API_KEY_DEFAULT = os.environ.get("KONACHAN_API_KEY", "").strip()
KONACHAN_USERNAME_DEFAULT = os.environ.get("KONACHAN_USERNAME", "").strip()
KONACHAN_PASSWORD_DEFAULT = os.environ.get("KONACHAN_PASSWORD", "").strip()
KONACHAN_EMAIL_DEFAULT = os.environ.get("KONACHAN_EMAIL", "").strip()

DEFAULT_PROXY = {
    "enabled": False,
    "type": "HTTP",
    "host": "",
    "port": None,
    "username": "",
    "password": "",
}


def default_api_config() -> dict:
    return {
        "rule34": {
            "userId": RULE34_USER_ID_DEFAULT,
            "apiKey": RULE34_API_KEY_DEFAULT,
        },
        "konachan": {
            "apiKey": KONACHAN_API_KEY_DEFAULT,
            "username": KONACHAN_USERNAME_DEFAULT,
            "password": KONACHAN_PASSWORD_DEFAULT,
            "email": KONACHAN_EMAIL_DEFAULT,
        },
        "ai": {
            "baseUrl": AI_BASE_URL_DEFAULT,
            "apiKey": AI_API_KEY_DEFAULT,
            "model": AI_MODEL_DEFAULT,
        },
    }


DEFAULT_PREFERENCES = {
    "preferredTags": [],
    "blockedTags": [],
}

DEFAULT_STATE = {
    "favorites": [],
    "savedSearches": [],
    "proxy": DEFAULT_PROXY,
    "apiConfig": default_api_config(),
    "preferences": DEFAULT_PREFERENCES,
    "nextSavedSearchId": 1,
}

STATE_LOCK = threading.Lock()
TAG_CACHE_LOCK = threading.Lock()
RESOLVE_CACHE_LOCK = threading.Lock()
TAG_CACHE = {}
RESOLVE_CACHE = {}
SERVICE_IDS = {"rule34", "pornhub", "konachan", "xbooru", "tbib", "eporner", "redtube"}
JSON_HEADERS = {
    "Content-Type": "application/json; charset=utf-8",
    "Cache-Control": "no-store",
    "Access-Control-Allow-Origin": "*",
    "Access-Control-Allow-Methods": "GET, POST, OPTIONS",
    "Access-Control-Allow-Headers": "Content-Type",
}
HTTP_TIMEOUT = 35
USER_AGENT = "R34RuleServer/1.0"
KONACHAN_BROWSER_USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) "
    "Chrome/123.0.0.0 Safari/537.36"
)
DEFAULT_PREFERENCE_CATALOG = [
    {"tag": "dickgirl", "titleRu": "Девочка с хуем"},
    {"tag": "futanari", "titleRu": "Футанари"},
    {"tag": "big_breasts", "titleRu": "Большие сиськи"},
    {"tag": "large_breasts", "titleRu": "Большие сиськи"},
    {"tag": "huge_breasts", "titleRu": "Огромные сиськи"},
    {"tag": "gigantic_breasts", "titleRu": "Гигантские сиськи"},
    {"tag": "massive_breasts", "titleRu": "Массивные сиськи"},
    {"tag": "enormous_breasts", "titleRu": "Нереально огромные сиськи"},
    {"tag": "small_breasts", "titleRu": "Маленькие сиськи"},
    {"tag": "flat_chest", "titleRu": "Плоская грудь"},
    {"tag": "big_ass", "titleRu": "Большая жопа"},
    {"tag": "large_ass", "titleRu": "Большая жопа"},
    {"tag": "huge_ass", "titleRu": "Огромная жопа"},
    {"tag": "small_ass", "titleRu": "Маленькая жопа"},
    {"tag": "ass_focus", "titleRu": "Фокус на жопе"},
    {"tag": "breast_focus", "titleRu": "Фокус на сиськах"},
    {"tag": "wide_hips", "titleRu": "Широкие бёдра"},
    {"tag": "thick_thighs", "titleRu": "Толстые бёдра"},
    {"tag": "thick_hips", "titleRu": "Массивные бёдра"},
    {"tag": "curvy", "titleRu": "Фигуристая"},
    {"tag": "voluptuous", "titleRu": "Очень пышная фигура"},
    {"tag": "petite", "titleRu": "Миниатюрная"},
    {"tag": "slim", "titleRu": "Худая"},
    {"tag": "skinny", "titleRu": "Очень худая"},
    {"tag": "chubby", "titleRu": "Полноватая"},
    {"tag": "plump", "titleRu": "Пышка"},
    {"tag": "bbw", "titleRu": "Очень полная"},
    {"tag": "muscular_female", "titleRu": "Мускулистая девушка"},
    {"tag": "female_muscle", "titleRu": "Выраженные женские мышцы"},
    {"tag": "athletic_female", "titleRu": "Спортивная девушка"},
    {"tag": "anal", "titleRu": "Анальный секс"},
    {"tag": "oral", "titleRu": "Оральный секс"},
    {"tag": "paizuri", "titleRu": "Сиськотряс / паизури"},
    {"tag": "blowjob", "titleRu": "Минет"},
    {"tag": "deepthroat", "titleRu": "Глубокий минет"},
    {"tag": "handjob", "titleRu": "Дрочка рукой"},
    {"tag": "footjob", "titleRu": "Дрочка ногами"},
    {"tag": "rimjob", "titleRu": "Вылизывание жопы"},
    {"tag": "facesitting", "titleRu": "Сидит лицом на лице"},
    {"tag": "cum", "titleRu": "Сперма"},
    {"tag": "cum_on_face", "titleRu": "Сперма на лице"},
    {"tag": "cum_in_mouth", "titleRu": "Сперма во рту"},
    {"tag": "cum_in_pussy", "titleRu": "Кончают в пизду"},
    {"tag": "cum_on_body", "titleRu": "Сперма по телу"},
    {"tag": "creampie", "titleRu": "Кремпай"},
    {"tag": "bukkake", "titleRu": "Буккаке"},
    {"tag": "masturbation", "titleRu": "Мастурбация"},
    {"tag": "sex_from_behind", "titleRu": "Трахают сзади"},
    {"tag": "doggystyle", "titleRu": "Доггистайл"},
    {"tag": "missionary", "titleRu": "Миссионерская поза"},
    {"tag": "cowgirl_position", "titleRu": "Наездница"},
    {"tag": "reverse_cowgirl_position", "titleRu": "Обратная наездница"},
    {"tag": "double_penetration", "titleRu": "Двойное проникновение"},
    {"tag": "group_sex", "titleRu": "Групповой секс"},
    {"tag": "gangbang", "titleRu": "Гэнгбэнг"},
    {"tag": "milf", "titleRu": "Зрелая женщина"},
    {"tag": "teen", "titleRu": "Молодая / teen"},
    {"tag": "mature", "titleRu": "Зрелая"},
    {"tag": "amateur", "titleRu": "Любительское"},
    {"tag": "lesbian", "titleRu": "Лесбиянки"},
    {"tag": "threesome", "titleRu": "Тройничок"},
    {"tag": "interracial", "titleRu": "Межрасовое"},
    {"tag": "asian", "titleRu": "Азиатки"},
    {"tag": "japanese", "titleRu": "Японки"},
    {"tag": "latina", "titleRu": "Латинки"},
    {"tag": "ebony", "titleRu": "Темнокожие"},
    {"tag": "massage", "titleRu": "Массаж"},
    {"tag": "squirting", "titleRu": "Сквирт"},
    {"tag": "hentai", "titleRu": "Хентай"},
    {"tag": "nurse", "titleRu": "Медсестра"},
    {"tag": "maid", "titleRu": "Горничная"},
    {"tag": "school_uniform", "titleRu": "Школьная форма"},
    {"tag": "cosplay", "titleRu": "Косплей"},
    {"tag": "monster_girl", "titleRu": "Девушка-монстр"},
    {"tag": "elf", "titleRu": "Эльфийка"},
    {"tag": "tentacles", "titleRu": "Тентакли"},
    {"tag": "pregnant", "titleRu": "Беременность"},
    {"tag": "rape", "titleRu": "Изнасилование"},
    {"tag": "bondage", "titleRu": "Бондаж"},
    {"tag": "femdom", "titleRu": "Женское доминирование"},
    {"tag": "pegging", "titleRu": "Пеггинг"},
    {"tag": "yaoi", "titleRu": "Яой"},
    {"tag": "yuri", "titleRu": "Юри"},
    {"tag": "nude", "titleRu": "Нагота"},
    {"tag": "sex", "titleRu": "Секс"},
    {"tag": "ahegao", "titleRu": "Похотливая рожа"},
    {"tag": "pov", "titleRu": "От первого лица"},
    {"tag": "stockings", "titleRu": "Чулки"},
    {"tag": "pantyhose", "titleRu": "Колготки"},
    {"tag": "dark_skin", "titleRu": "Тёмная кожа"},
    {"tag": "tanlines", "titleRu": "Следы от загара"},
    {"tag": "tattoo", "titleRu": "Татуировки"},
    {"tag": "piercing", "titleRu": "Пирсинг"},
    {"tag": "hairy_pussy", "titleRu": "Волосатая пизда"},
    {"tag": "public_sex", "titleRu": "Секс на публике"},
    {"tag": "shower_sex", "titleRu": "Секс в душе"},
]


def now_ms() -> int:
    return int(time.time() * 1000)


def ensure_data_root() -> None:
    DATA_ROOT.mkdir(parents=True, exist_ok=True)
    if not STATE_FILE.exists():
        serializable = {
            "favorites": [normalize_post(item) for item in DEFAULT_STATE.get("favorites") or []],
            "savedSearches": [normalize_saved_search(item) for item in DEFAULT_STATE.get("savedSearches") or []],
            "proxy": normalize_proxy(DEFAULT_STATE.get("proxy") or {}),
            "apiConfig": normalize_api_config(DEFAULT_STATE.get("apiConfig") or {}),
            "preferences": normalize_preferences(DEFAULT_STATE.get("preferences") or {}),
            "nextSavedSearchId": int(DEFAULT_STATE.get("nextSavedSearchId") or 1),
        }
        STATE_FILE.write_text(json.dumps(serializable, ensure_ascii=False, indent=2), "utf-8")


def load_state() -> dict:
    ensure_data_root()
    try:
        data = json.loads(STATE_FILE.read_text("utf-8"))
    except Exception:
        data = {}
    state = DEFAULT_STATE | data
    state["favorites"] = [normalize_post(item) for item in state.get("favorites") or []]
    state["savedSearches"] = [normalize_saved_search(item) for item in state.get("savedSearches") or []]
    state["proxy"] = normalize_proxy(state.get("proxy") or {})
    state["apiConfig"] = normalize_api_config(state.get("apiConfig") or {})
    state["preferences"] = normalize_preferences(state.get("preferences") or {})
    state["nextSavedSearchId"] = max(
        int(state.get("nextSavedSearchId") or 1),
        max((int(item.get("id") or 0) for item in state["savedSearches"]), default=0) + 1,
    )
    return state


def save_state(state: dict) -> None:
    ensure_data_root()
    serializable = {
        "favorites": [normalize_post(item) for item in state.get("favorites") or []],
        "savedSearches": [normalize_saved_search(item) for item in state.get("savedSearches") or []],
        "proxy": normalize_proxy(state.get("proxy") or {}),
        "apiConfig": normalize_api_config(state.get("apiConfig") or {}),
        "preferences": normalize_preferences(state.get("preferences") or {}),
        "nextSavedSearchId": int(state.get("nextSavedSearchId") or 1),
    }
    temp_path = STATE_FILE.with_suffix(".tmp")
    temp_path.write_text(json.dumps(serializable, ensure_ascii=False, indent=2), "utf-8")
    temp_path.replace(STATE_FILE)


def normalize_service_id(value: str | None) -> str:
    service_id = str(value or "").strip().lower()
    service_id = service_id.split(".")[-1]
    service_id = re.sub(r"[^a-z0-9_]+", "", service_id)
    service_id = {
        "rule": "rule34",
        "r34": "rule34",
        "rule34xxx": "rule34",
        "pornhubcom": "pornhub",
        "konachancom": "konachan",
        "xboorucom": "xbooru",
        "tbiborg": "tbib",
        "epornercom": "eporner",
        "redtubecom": "redtube",
    }.get(service_id, service_id)
    if service_id not in SERVICE_IDS:
        raise ValueError("Неизвестный сервис.")
    return service_id


def normalize_media_type(value: str | None) -> str:
    media_type = str(value or "").strip().upper()
    if media_type in {"IMAGE", "VIDEO"}:
        return media_type
    return "UNKNOWN"


def normalize_tags(value) -> list[str]:
    if isinstance(value, list):
        items = value
    else:
        items = str(value or "").split(" ")
    return [str(item).strip() for item in items if str(item).strip()]


def normalize_post(raw: dict) -> dict:
    service_id = normalize_service_id(raw.get("serviceId") or raw.get("service"))
    post_id = str(raw.get("id") or "").strip()
    if not post_id:
        raise ValueError("Некорректный post id.")
    file_url = str(raw.get("fileUrl") or "").strip()
    if not file_url:
        raise ValueError("Некорректный fileUrl.")
    return {
        "serviceId": service_id,
        "id": post_id,
        "serviceScopedId": f"{service_id}:{post_id}",
        "title": str(raw.get("title") or "").strip(),
        "previewUrl": str(raw.get("previewUrl") or "").strip() or None,
        "sampleUrl": str(raw.get("sampleUrl") or "").strip() or None,
        "fileUrl": file_url,
        "pageUrl": str(raw.get("pageUrl") or "").strip() or None,
        "embedUrl": str(raw.get("embedUrl") or "").strip() or None,
        "tags": normalize_tags(raw.get("tags")),
        "rating": str(raw.get("rating") or "").strip(),
        "score": int(raw.get("score") or 0),
        "width": int(raw.get("width") or 0),
        "height": int(raw.get("height") or 0),
        "mediaType": normalize_media_type(raw.get("mediaType")),
        "hasDirectMedia": bool(raw.get("hasDirectMedia", True)),
        "savedAt": int(raw.get("savedAt") or now_ms()),
    }


def normalize_saved_search(raw: dict) -> dict:
    search_id = int(raw.get("id") or 0)
    return {
        "id": search_id,
        "serviceId": normalize_service_id(raw.get("serviceId") or raw.get("service")),
        "query": str(raw.get("query") or "").strip(),
        "label": str(raw.get("label") or "").strip() or str(raw.get("query") or "").strip(),
        "createdAt": int(raw.get("createdAt") or now_ms()),
    }


def normalize_proxy(raw: dict) -> dict:
    proxy_type = str(raw.get("type") or "HTTP").strip().upper()
    if proxy_type not in {"HTTP", "SOCKS"}:
        proxy_type = "HTTP"
    port_value = raw.get("port")
    port = int(port_value) if isinstance(port_value, int) or str(port_value or "").isdigit() else None
    if port is not None and not (1 <= port <= 65535):
        port = None
    return {
        "enabled": bool(raw.get("enabled")),
        "type": proxy_type,
        "host": str(raw.get("host") or "").strip(),
        "port": port,
        "username": str(raw.get("username") or "").strip(),
        "password": str(raw.get("password") or ""),
    }


def normalize_api_config(raw: dict) -> dict:
    defaults = default_api_config()
    rule34 = raw.get("rule34") or {}
    konachan = raw.get("konachan") or {}
    ai = raw.get("ai") or {}
    return {
        "rule34": {
            "userId": str(rule34.get("userId") or defaults["rule34"]["userId"]).strip(),
            "apiKey": str(rule34.get("apiKey") or defaults["rule34"]["apiKey"]).strip(),
        },
        "konachan": {
            "apiKey": str(konachan.get("apiKey") or defaults["konachan"]["apiKey"]).strip(),
            "username": str(konachan.get("username") or defaults["konachan"]["username"]).strip(),
            "password": str(konachan.get("password") or defaults["konachan"]["password"]),
            "email": str(konachan.get("email") or defaults["konachan"]["email"]).strip(),
        },
        "ai": {
            "baseUrl": str(ai.get("baseUrl") or defaults["ai"]["baseUrl"]).strip().rstrip("/"),
            "apiKey": str(ai.get("apiKey") or defaults["ai"]["apiKey"]).strip(),
            "model": str(ai.get("model") or defaults["ai"]["model"]).strip() or defaults["ai"]["model"],
        },
    }


def normalize_preferences(raw: dict) -> dict:
    preferred = [normalize_candidate_tag(item) for item in raw.get("preferredTags") or []]
    preferred = [item for item in preferred if item]
    blocked = [normalize_candidate_tag(item) for item in raw.get("blockedTags") or []]
    blocked = [item for item in blocked if item]
    blocked = dedupe_preserve(blocked)
    preferred = [item for item in preferred if item not in blocked]
    return {
        "preferredTags": dedupe_preserve(preferred),
        "blockedTags": blocked,
    }


def preference_title_ru(tag: str) -> str:
    normalized = normalize_candidate_tag(tag)
    manual = next((item["titleRu"] for item in DEFAULT_PREFERENCE_CATALOG if item["tag"] == normalized), None)
    if manual:
        return manual
    return normalized.replace("_", " ").strip().capitalize()


def preference_titles_map(tags: list[str]) -> dict:
    return {
        tag: preference_title_ru(tag)
        for tag in dedupe_preserve([normalize_candidate_tag(item) for item in tags if normalize_candidate_tag(item)])
    }


def public_state(state: dict) -> dict:
    favorites = sorted(state.get("favorites") or [], key=lambda item: int(item.get("savedAt") or 0), reverse=True)
    saved_searches = sorted(
        state.get("savedSearches") or [],
        key=lambda item: int(item.get("createdAt") or 0),
        reverse=True,
    )
    preferences = normalize_preferences(state.get("preferences") or {})
    all_preference_tags = preferences.get("preferredTags", []) + preferences.get("blockedTags", [])
    return {
        "favorites": favorites,
        "favoriteIds": [item["serviceScopedId"] for item in favorites],
        "savedSearches": saved_searches,
        "proxy": normalize_proxy(state.get("proxy") or {}),
        "apiConfig": normalize_api_config(state.get("apiConfig") or {}),
        "preferences": preferences,
        "preferenceCatalog": default_preference_catalog(),
        "preferenceTitles": preference_titles_map(all_preference_tags),
    }

def read_url(
    url: str,
    headers: dict | None = None,
    use_proxy: bool = True,
    data: bytes | None = None,
    method: str | None = None,
    timeout: int = HTTP_TIMEOUT,
) -> bytes:
    request = urllib.request.Request(
        url,
        data=data,
        method=method,
        headers={"User-Agent": USER_AGENT, **(headers or {})},
    )
    opener = urllib.request.build_opener()
    if not use_proxy:
        # ProxyHandler(None) disables environment proxies without breaking
        # TLS handshakes on providers like Konachan.
        opener = urllib.request.build_opener(urllib.request.ProxyHandler(None))
    with opener.open(request, timeout=timeout) as response:
        return response.read()


def read_json_url(
    url: str,
    headers: dict | None = None,
    use_proxy: bool = True,
    data: bytes | None = None,
    method: str | None = None,
    timeout: int = HTTP_TIMEOUT,
):
    return json.loads(
        read_url(url, headers=headers, use_proxy=use_proxy, data=data, method=method, timeout=timeout).decode("utf-8"),
    )


def proxy_url_from_config(proxy_config: dict | None) -> str | None:
    proxy = normalize_proxy(proxy_config or {})
    if not proxy.get("enabled") or not proxy.get("host") or not proxy.get("port"):
        return None
    scheme = "socks5h" if proxy.get("type") == "SOCKS" else "http"
    username = urllib.parse.quote(str(proxy.get("username") or ""), safe="")
    password = urllib.parse.quote(str(proxy.get("password") or ""), safe="")
    credentials = f"{username}:{password}@" if username or password else ""
    return f"{scheme}://{credentials}{proxy['host']}:{proxy['port']}"


def read_json_url_via_curl(
    url: str,
    headers: dict | None = None,
    proxy_config: dict | None = None,
    timeout: int = HTTP_TIMEOUT,
):
    command = [
        "curl",
        "--silent",
        "--show-error",
        "--location",
        "--max-time",
        str(timeout),
    ]
    proxy_url = proxy_url_from_config(proxy_config)
    if proxy_url:
        command.extend(["--proxy", proxy_url])
    else:
        command.extend(["--noproxy", "*"])
    for key, value in (headers or {}).items():
        command.extend(["-H", f"{key}: {value}"])
    command.append(url)
    output = subprocess.check_output(command, text=True)
    return json.loads(output)


def query_rule34_tags(term: str, api_config: dict) -> list[dict]:
    params_data = {
        "page": "dapi",
        "s": "tag",
        "q": "index",
        "limit": "100",
        "name_pattern": f"%{term}%",
    }
    rule34_config = normalize_api_config(api_config).get("rule34") or {}
    user_id = str(rule34_config.get("userId") or "").strip()
    api_key = str(rule34_config.get("apiKey") or "").strip()
    if not user_id or not api_key:
        return []
    params_data["user_id"] = user_id
    params_data["api_key"] = api_key
    params = urllib.parse.urlencode(params_data)
    root = ET.fromstring(read_url(f"https://api.rule34.xxx/index.php?{params}", use_proxy=False).decode("utf-8"))
    result = []
    for item in root.findall("tag"):
        result.append(
            {
                "name": item.attrib.get("name", "").strip(),
                "count": int(item.attrib.get("count") or 0),
            },
        )
    return result


def query_xbooru_tags(term: str) -> list[dict]:
    params = urllib.parse.urlencode(
        {
            "page": "dapi",
            "s": "tag",
            "q": "index",
            "limit": "100",
            "name_pattern": f"%{term}%",
        },
    )
    root = ET.fromstring(read_url(f"https://xbooru.com/index.php?{params}", use_proxy=False).decode("utf-8"))
    result = []
    for item in root.findall("tag"):
        result.append(
            {
                "name": item.attrib.get("name", "").strip(),
                "count": int(item.attrib.get("count") or 0),
            },
        )
    return result


def query_tbib_tags(term: str) -> list[dict]:
    params = urllib.parse.urlencode(
        {
            "page": "dapi",
            "s": "tag",
            "q": "index",
            "limit": "100",
            "name_pattern": f"%{term}%",
        },
    )
    root = ET.fromstring(
        read_url(
            f"https://tbib.org/index.php?{params}",
            headers={"User-Agent": KONACHAN_BROWSER_USER_AGENT},
            use_proxy=False,
        ).decode("utf-8"),
    )
    result = []
    for item in root.findall("tag"):
        result.append(
            {
                "name": item.attrib.get("name", "").strip(),
                "count": int(item.attrib.get("count") or 0),
            },
        )
    return result


def query_pornhub_terms(term: str) -> list[dict]:
    normalized = str(term or "").strip().lower()
    if not normalized:
        return []
    result = []
    seen = set()

    categories_data = read_json_url("https://www.pornhub.com/webmasters/categories", use_proxy=False)
    for item in categories_data.get("categories") or []:
        raw_name = str((item or {}).get("category") or "").replace("-1", "").strip()
        if not raw_name or normalized not in raw_name.lower():
            continue
        candidate = normalize_candidate_tag(raw_name)
        if not candidate or candidate in seen:
            continue
        seen.add(candidate)
        result.append({"name": raw_name, "count": 0})
    return result


def query_redtube_terms(term: str) -> list[dict]:
    normalized = str(term or "").strip().lower()
    if not normalized:
        return []
    result = []
    seen = set()

    categories_data = read_json_url(
        "https://api.redtube.com/?data=redtube.Categories.getCategoriesList&output=json",
        use_proxy=False,
    )
    for item in categories_data.get("categories") or []:
        raw_name = str((item or {}).get("category") or "").strip()
        if not raw_name or normalized not in raw_name.lower():
            continue
        candidate = normalize_candidate_tag(raw_name)
        if not candidate or candidate in seen:
            continue
        seen.add(candidate)
        result.append({"name": raw_name, "count": 0})
    return result


def query_eporner_terms(term: str) -> list[dict]:
    normalized = str(term or "").strip().lower()
    if not normalized:
        return []

    data = read_json_url(
        (
            "https://www.eporner.com/api/v2/video/search/"
            f"?query={urllib.parse.quote(normalized)}&per_page=24&page=1&format=json"
        ),
        use_proxy=False,
    )

    ranked = {}
    for item in data.get("videos") or []:
        keywords = str(item.get("keywords") or "")
        for raw_name in keywords.split(","):
            candidate_name = raw_name.strip()
            if not candidate_name or normalized not in candidate_name.lower():
                continue
            candidate = normalize_candidate_tag(candidate_name)
            if not candidate:
                continue
            ranked[candidate_name] = ranked.get(candidate_name, 0) + 1

    return [
        {"name": name, "count": count}
        for name, count in sorted(ranked.items(), key=lambda item: (-item[1], item[0].lower()))
    ]


def query_konachan_tags(term: str, api_config: dict, proxy_config: dict | None = None) -> list[dict]:
    params = {
        "name": f"*{term}*",
        "limit": 100,
    }
    konachan_config = normalize_api_config(api_config).get("konachan") or {}
    if str(konachan_config.get("apiKey") or "").strip():
        params["api_key"] = str(konachan_config["apiKey"]).strip()
    url = f"https://konachan.com/tag.json?{urllib.parse.urlencode(params)}"
    headers = {
        "User-Agent": KONACHAN_BROWSER_USER_AGENT,
        "Accept": "application/json",
    }
    try:
        data = read_json_url(
            url,
            headers=headers,
            use_proxy=False,
        )
    except Exception:
        if proxy_url_from_config(proxy_config) is None:
            raise
        data = read_json_url_via_curl(
            url,
            headers=headers,
            proxy_config=proxy_config,
        )
    return [
        {
            "name": str(item.get("name") or "").strip(),
            "count": int(item.get("count") or 0),
        }
        for item in data
    ]


def fetch_service_tags(service_id: str, term: str, api_config: dict, proxy_config: dict | None = None) -> list[dict]:
    normalized_term = str(term or "").strip().lower()
    if not normalized_term:
        return []
    cache_key = (service_id, normalized_term)
    with TAG_CACHE_LOCK:
        cached = TAG_CACHE.get(cache_key)
    if cached is not None:
        return cached

    try:
        if service_id == "rule34":
            result = query_rule34_tags(normalized_term, api_config)
        elif service_id == "konachan":
            result = query_konachan_tags(normalized_term, api_config, proxy_config)
        elif service_id == "xbooru":
            result = query_xbooru_tags(normalized_term)
        elif service_id == "tbib":
            result = query_tbib_tags(normalized_term)
        elif service_id == "pornhub":
            result = query_pornhub_terms(normalized_term)
        elif service_id == "redtube":
            result = query_redtube_terms(normalized_term)
        elif service_id == "eporner":
            result = query_eporner_terms(normalized_term)
        else:
            result = []
    except Exception as exc:
        print(
            f"Tag fetch failed for service={service_id} term={normalized_term}: {exc}",
            flush=True,
        )
        return []

    with TAG_CACHE_LOCK:
        TAG_CACHE[cache_key] = result
    return result


def default_preference_catalog() -> list[dict]:
    return [
        {
            "tag": item["tag"],
            "titleRu": item["titleRu"],
            "postCount": 0,
        }
        for item in DEFAULT_PREFERENCE_CATALOG
    ]


def search_preference_catalog(
    service_id: str,
    raw_query: str,
    api_config: dict,
    proxy_config: dict | None = None,
) -> list[dict]:
    query = str(raw_query or "").strip()
    if not query:
        return default_preference_catalog()

    items = []
    seen = set()
    prefer_local_first = service_id in {"pornhub", "redtube", "eporner", "tbib"}

    def append_local_matches():
        for item in default_preference_catalog():
            if query.lower() not in item["tag"] and query.lower() not in item["titleRu"].lower():
                continue
            if item["tag"] in seen:
                continue
            seen.add(item["tag"])
            items.append(item)
            if len(items) >= 30:
                break

    if prefer_local_first:
        append_local_matches()

    for item in fetch_service_tags(service_id, query, api_config, proxy_config):
        tag = normalize_candidate_tag(item.get("name", ""))
        if not tag or tag in seen:
            continue
        seen.add(tag)
        items.append(
            {
                "tag": tag,
                "titleRu": preference_title_ru(tag),
                "postCount": int(item.get("count") or 0),
            },
        )
        if len(items) >= 30:
            break

    if len(items) < 30:
        append_local_matches()

    return items[:30]


def looks_like_tag_query(raw_query: str) -> bool:
    query = raw_query.strip()
    if not query:
        return False
    if re.search(r"[А-Яа-яЁё]", query):
        return False
    if any(char in query for char in [":", "-", "(", ")", "*"]):
        return True
    return any("_" in token for token in query.split())


def normalize_candidate_tag(value: str) -> str:
    normalized = re.sub(r"[^a-z0-9_()]+", "_", value.lower()).strip("_")
    return re.sub(r"_+", "_", normalized)


def slugify_name_token(value: str) -> str:
    cleaned = re.sub(r"[^a-z0-9]+", "_", value.lower())
    return re.sub(r"_+", "_", cleaned).strip("_")


def name_candidates_from_canonical(canonical_name: str) -> list[str]:
    tokens = [slugify_name_token(item) for item in re.findall(r"[A-Za-z0-9]+", canonical_name or "")]
    tokens = [item for item in tokens if item]
    if not tokens:
        return []
    candidates = []
    joined = "_".join(tokens)
    if joined:
        candidates.append(joined)
    if len(tokens) >= 2:
        candidates.append(f"{tokens[0]}_{tokens[-1]}")
        candidates.append(f"{tokens[-1]}_{tokens[0]}")
    if len(tokens) == 3:
        candidates.append(f"{tokens[0]}_{tokens[1]}_{tokens[2]}")
        candidates.append(f"{tokens[2]}_{tokens[0]}_{tokens[1]}")
    return [item for item in candidates if item]


def preferred_fallback_candidates(service_id: str, canonical_name: str) -> list[str]:
    tokens = [slugify_name_token(item) for item in re.findall(r"[A-Za-z0-9]+", canonical_name or "")]
    tokens = [item for item in tokens if item]
    if not tokens:
        return []
    if len(tokens) == 1:
        return [tokens[0]]

    first_last = f"{tokens[0]}_{tokens[-1]}"
    last_first = f"{tokens[-1]}_{tokens[0]}"
    if service_id in {"rule34", "konachan"}:
        return [last_first, first_last]
    return [first_last, last_first]


def resolve_exact_tag_count(
    service_id: str,
    candidate: str,
    api_config: dict,
    proxy_config: dict | None = None,
) -> int:
    normalized_candidate = normalize_candidate_tag(candidate)
    if not normalized_candidate:
        return 0
    search_term = normalized_candidate.split("_")[0]
    for item in fetch_service_tags(service_id, search_term, api_config, proxy_config):
        if normalize_candidate_tag(item.get("name", "")) == normalized_candidate:
            return int(item.get("count") or 0)
    return 0


def ask_ai(service_id: str, raw_query: str, api_config: dict) -> dict:
    prompt = (
        "You recover the intended canonical Latin name of a fictional character, franchise, or booru-friendly concept from a noisy user query. "
        "The query may be misspelled, partially transliterated from Russian, or phonetically distorted. "
        "Do NOT keep the typo if a well-known anime/game/manga character is a better match. "
        "Use web knowledge and search if available. "
        "Then propose booru tag candidates for the selected service. "
        "Prefer the popular canonical character over literal transliteration. "
        "For two-part names, include both first_last and last_first in candidate_tags when plausible because different booru sites may prefer different order. "
        "Example: query 'Ручими Курикими' should resolve to canonical_name 'Rukia Kuchiki', not 'Ruchimi Kurikimi'. "
        "Return JSON only: "
        "{\"canonical_name\":\"...\",\"candidate_tags\":[\"...\"],\"franchise_tags\":[\"...\"],\"explanation_ru\":\"...\"}. "
        f"Selected service: {service_id}. "
        f"Raw query: {raw_query}"
    )
    payload = json.dumps(
        {
            "model": normalize_api_config(api_config).get("ai", {}).get("model") or AI_MODEL_DEFAULT,
            "temperature": 0.1,
            "messages": [
                {
                    "role": "user",
                    "content": prompt,
                },
            ],
        },
        ensure_ascii=False,
    ).encode("utf-8")
    ai_config = normalize_api_config(api_config).get("ai") or {}
    base_url = str(ai_config.get("baseUrl") or AI_BASE_URL_DEFAULT).strip().rstrip("/")
    api_key = str(ai_config.get("apiKey") or "").strip()
    if not base_url or not api_key:
        raise ValueError("AI API на сервере не настроен.")
    data = read_json_url(
        base_url,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        use_proxy=False,
        data=payload,
        method="POST",
        timeout=120,
    )
    content = data["choices"][0]["message"]["content"]
    cleaned = extract_json_object(str(content))
    return json.loads(cleaned)


def extract_json_object(content: str) -> str:
    cleaned = str(content).replace("```json", "").replace("```", "").strip()
    start = cleaned.find("{")
    end = cleaned.rfind("}")
    if start == -1 or end == -1 or end <= start:
        raise ValueError("AI не вернул JSON-объект.")
    return cleaned[start:end + 1]


def dedupe_preserve(items: list[str]) -> list[str]:
    seen = set()
    result = []
    for item in items:
        normalized = item.strip()
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        result.append(normalized)
    return result


def resolve_query(
    service_id: str,
    raw_query: str,
    mode: str,
    api_config: dict,
    proxy_config: dict | None = None,
) -> dict:
    normalized_query = str(raw_query or "").strip()
    if not normalized_query:
        raise ValueError("Введите запрос.")

    cache_key = (service_id, mode, normalized_query.lower())
    with RESOLVE_CACHE_LOCK:
        cached = RESOLVE_CACHE.get(cache_key)
    if cached is not None:
        return cached

    text_search_service = service_id in {"pornhub", "eporner", "redtube"}

    simple_candidate = normalize_candidate_tag(normalized_query.replace(" ", "_"))
    if (
        mode == "auto"
        and not text_search_service
        and simple_candidate
        and resolve_exact_tag_count(service_id, simple_candidate, api_config, proxy_config) > 0
    ):
        resolved = {
            "resolvedQuery": simple_candidate,
            "canonicalName": None,
            "tags": [simple_candidate],
            "explanation": (
                "Запрос уже совпал с тегом выбранного сервиса."
                if looks_like_tag_query(normalized_query)
                else "Нашёл точный тег на выбранном сервисе."
            ),
            "source": "direct-exact",
            "serviceId": service_id,
        }
        with RESOLVE_CACHE_LOCK:
            RESOLVE_CACHE[cache_key] = resolved
        return resolved

    ai_data = ask_ai(service_id, normalized_query, api_config)
    canonical_name = str(ai_data.get("canonical_name") or "").strip() or None
    ai_candidates = [
        normalize_candidate_tag(item)
        for item in ai_data.get("candidate_tags") or []
        if normalize_candidate_tag(str(item))
    ]
    franchise_tags = [
        normalize_candidate_tag(item)
        for item in ai_data.get("franchise_tags") or []
        if normalize_candidate_tag(str(item))
    ]
    explanation = str(ai_data.get("explanation_ru") or "").strip() or None

    if text_search_service:
        resolved_query = (
            canonical_name
            or next((item.replace("_", " ") for item in ai_candidates if item), "")
            or normalized_query
        ).strip()
        resolved = {
            "resolvedQuery": resolved_query or normalized_query,
            "canonicalName": canonical_name,
            "tags": dedupe_preserve([item.replace("_", " ") for item in ai_candidates][:3]),
            "explanation": explanation,
            "source": "text-search",
            "serviceId": service_id,
        }
        with RESOLVE_CACHE_LOCK:
            RESOLVE_CACHE[cache_key] = resolved
        return resolved

    exact_candidates = dedupe_preserve(name_candidates_from_canonical(canonical_name or "") + ai_candidates)
    best_tag = None
    best_count = -1
    for candidate in exact_candidates:
        count = resolve_exact_tag_count(service_id, candidate, api_config, proxy_config)
        if count > best_count:
            best_tag = candidate
            best_count = count

    if best_tag and best_count > 0:
        resolved_query = best_tag
        final_tags = [best_tag] + [item for item in franchise_tags if item and item != best_tag][:2]
        source = "validated"
    else:
        token_pool = dedupe_preserve(
            [
                *(slugify_name_token(item) for item in re.findall(r"[A-Za-z0-9]+", canonical_name or "")),
                *(item.split("_")[0] for item in ai_candidates if item),
                *(slugify_name_token(item) for item in re.findall(r"[A-Za-z0-9]+", normalized_query)),
            ],
        )
        ranked = {}
        for term in token_pool[:4]:
            for item in fetch_service_tags(service_id, term, api_config, proxy_config):
                name = normalize_candidate_tag(item.get("name", ""))
                count = int(item.get("count") or 0)
                if not name:
                    continue
                ranked[name] = max(ranked.get(name, 0), count)
        if ranked:
            best_tag = max(ranked.items(), key=lambda item: item[1])[0]
            resolved_query = best_tag
            final_tags = [best_tag] + [item for item in franchise_tags if item and item != best_tag][:2]
            source = "loose"
        else:
            preferred = preferred_fallback_candidates(service_id, canonical_name or "")
            fallback = (
                preferred[0]
                if preferred
                else ai_candidates[0] if ai_candidates
                else normalize_candidate_tag(normalized_query.replace(" ", "_"))
            )
            resolved_query = fallback or normalized_query
            final_tags = [resolved_query] + franchise_tags[:2]
            source = "fallback"

    resolved = {
        "resolvedQuery": resolved_query,
        "canonicalName": canonical_name,
        "tags": dedupe_preserve(final_tags),
        "explanation": explanation,
        "source": source,
        "serviceId": service_id,
    }
    with RESOLVE_CACHE_LOCK:
        RESOLVE_CACHE[cache_key] = resolved
    return resolved


class RuleHandler(BaseHTTPRequestHandler):
    server_version = "RuleServer/1.0"

    def log_message(self, fmt, *args):
        return

    def do_OPTIONS(self):
        self.send_response(204)
        for key, value in JSON_HEADERS.items():
            self.send_header(key, value)
        self.end_headers()

    def do_GET(self):
        path = urllib.parse.urlsplit(self.path).path
        if path == "/" or path == "":
            self.send_html(
                200,
                (
                    "<!doctype html><html lang='ru'><head><meta charset='utf-8'>"
                    "<title>R34 Rule API</title></head><body style='font-family:sans-serif;background:#111;color:#f5f5f5;'>"
                    "<main style='max-width:720px;margin:48px auto;padding:24px;'>"
                    "<h1>/rule API</h1>"
                    "<p>Сервер для общих избранных, поисковых закладок, proxy и умного resolve booru-запросов.</p>"
                    f"<p>Health: <a style='color:#9bd' href='{PUBLIC_BASE_URL}/api/health'>{PUBLIC_BASE_URL}/api/health</a></p>"
                    "</main></body></html>"
                ),
            )
            return

        if path == "/api/health":
            with STATE_LOCK:
                state = load_state()
            ai_model = normalize_api_config(state.get("apiConfig") or {}).get("ai", {}).get("model") or AI_MODEL_DEFAULT
            self.send_json(200, {"ok": True, "time": int(time.time()), "model": ai_model})
            return

        if path == "/api/state":
            with STATE_LOCK:
                state = load_state()
            self.send_json(200, public_state(state))
            return

        if path == "/api/proxy":
            with STATE_LOCK:
                state = load_state()
            self.send_json(200, {"proxy": normalize_proxy(state.get("proxy") or {})})
            return

        if path == "/api/api-config":
            with STATE_LOCK:
                state = load_state()
            self.send_json(200, {"apiConfig": normalize_api_config(state.get("apiConfig") or {})})
            return

        if path == "/api/preferences":
            with STATE_LOCK:
                state = load_state()
            preferences = normalize_preferences(state.get("preferences") or {})
            tags = preferences.get("preferredTags", []) + preferences.get("blockedTags", [])
            self.send_json(
                200,
                {
                    "preferences": preferences,
                    "preferenceTitles": preference_titles_map(tags),
                    "preferenceCatalog": default_preference_catalog(),
                },
            )
            return

        if path == "/api/preferences/catalog":
            query = urllib.parse.parse_qs(urllib.parse.urlsplit(self.path).query)
            raw_service_id = (query.get("serviceId") or query.get("service") or ["rule34"])[0]
            try:
                service_id = normalize_service_id(raw_service_id)
            except ValueError as exc:
                self.send_json(400, {"error": str(exc)})
                return
            term = (query.get("query") or [""])[0]
            with STATE_LOCK:
                state = load_state()
            items = search_preference_catalog(
                service_id,
                term,
                normalize_api_config(state.get("apiConfig") or {}),
                normalize_proxy(state.get("proxy") or {}),
            )
            self.send_json(
                200,
                {"items": items},
            )
            return

        self.send_json(404, {"error": "Not found"})

    def do_POST(self):
        path = urllib.parse.urlsplit(self.path).path
        body = self.read_json_body()
        if body is None:
            return

        try:
            if path == "/api/favorites/toggle":
                post = normalize_post(body)
                with STATE_LOCK:
                    state = load_state()
                    favorites = state["favorites"]
                    scoped_id = post["serviceScopedId"]
                    existing_index = next(
                        (index for index, item in enumerate(favorites) if item["serviceScopedId"] == scoped_id),
                        None,
                    )
                    if existing_index is None:
                        favorites.append(post)
                        changed = True
                    else:
                        favorites.pop(existing_index)
                        changed = False
                    state["favorites"] = favorites
                    save_state(state)
                self.send_json(200, {"changed": changed, "favorites": public_state(state)["favorites"], "favoriteIds": public_state(state)["favoriteIds"]})
                return

            if path == "/api/saved-searches":
                service_id = normalize_service_id(body.get("serviceId") or body.get("service"))
                query = str(body.get("query") or "").strip()
                label = str(body.get("label") or "").strip() or query
                if not query:
                    raise ValueError("Пустой запрос нельзя сохранить.")
                with STATE_LOCK:
                    state = load_state()
                    saved_searches = state["savedSearches"]
                    exists = any(
                        item["serviceId"] == service_id and item["query"].lower() == query.lower()
                        for item in saved_searches
                    )
                    saved = False
                    if not exists:
                        saved_searches.append(
                            {
                                "id": int(state["nextSavedSearchId"]),
                                "serviceId": service_id,
                                "query": query,
                                "label": label,
                                "createdAt": now_ms(),
                            },
                        )
                        state["nextSavedSearchId"] = int(state["nextSavedSearchId"]) + 1
                        save_state(state)
                        saved = True
                self.send_json(200, {"saved": saved, "savedSearches": public_state(state)["savedSearches"]})
                return

            if path == "/api/saved-searches/rename":
                search_id = int(body.get("id") or 0)
                label = str(body.get("label") or "").strip() or "Без названия"
                with STATE_LOCK:
                    state = load_state()
                    for item in state["savedSearches"]:
                        if int(item.get("id") or 0) == search_id:
                            item["label"] = label
                            break
                    save_state(state)
                self.send_json(200, {"savedSearches": public_state(state)["savedSearches"]})
                return

            if path == "/api/saved-searches/delete":
                search_id = int(body.get("id") or 0)
                with STATE_LOCK:
                    state = load_state()
                    state["savedSearches"] = [
                        item for item in state["savedSearches"] if int(item.get("id") or 0) != search_id
                    ]
                    save_state(state)
                self.send_json(200, {"savedSearches": public_state(state)["savedSearches"]})
                return

            if path == "/api/proxy":
                proxy = normalize_proxy(body)
                with STATE_LOCK:
                    state = load_state()
                    state["proxy"] = proxy
                    save_state(state)
                self.send_json(200, {"proxy": proxy})
                return

            if path == "/api/api-config":
                api_config = normalize_api_config(body)
                with STATE_LOCK:
                    state = load_state()
                    state["apiConfig"] = api_config
                    save_state(state)
                with TAG_CACHE_LOCK:
                    TAG_CACHE.clear()
                with RESOLVE_CACHE_LOCK:
                    RESOLVE_CACHE.clear()
                self.send_json(200, {"apiConfig": api_config})
                return

            if path == "/api/preferences":
                preferences = normalize_preferences(body)
                conflicts = set(preferences["preferredTags"]) & set(preferences["blockedTags"])
                if conflicts:
                    raise ValueError("Один и тот же тег нельзя одновременно любить и скрывать.")
                with STATE_LOCK:
                    state = load_state()
                    state["preferences"] = preferences
                    save_state(state)
                tags = preferences.get("preferredTags", []) + preferences.get("blockedTags", [])
                self.send_json(
                    200,
                    {
                        "preferences": preferences,
                        "preferenceTitles": preference_titles_map(tags),
                        "preferenceCatalog": default_preference_catalog(),
                    },
                )
                return

            if path == "/api/resolve-query":
                service_id = normalize_service_id(body.get("serviceId") or body.get("service"))
                mode = str(body.get("mode") or "auto").strip().lower()
                if mode not in {"auto", "ai"}:
                    mode = "auto"
                with STATE_LOCK:
                    state = load_state()
                result = resolve_query(
                    service_id,
                    str(body.get("query") or ""),
                    mode,
                    normalize_api_config(state.get("apiConfig") or {}),
                    normalize_proxy(state.get("proxy") or {}),
                )
                self.send_json(200, result)
                return
        except ValueError as exc:
            self.send_json(400, {"error": str(exc)})
            return
        except urllib.error.HTTPError as exc:
            self.send_json(502, {"error": f"Внешний API вернул HTTP {exc.code}."})
            return
        except Exception as exc:
            self.send_json(500, {"error": f"Внутренняя ошибка: {exc}"})
            return

        self.send_json(404, {"error": "Not found"})

    def read_json_body(self):
        content_length = self.headers.get("Content-Length", "0")
        try:
            length = int(content_length)
        except ValueError:
            length = 0
        raw = self.rfile.read(length) if length > 0 else b"{}"
        try:
            return json.loads(raw.decode("utf-8"))
        except Exception:
            self.send_json(400, {"error": "Невалидный JSON."})
            return None

    def send_json(self, status_code: int, payload: dict):
        data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        self.send_response(status_code)
        for key, value in JSON_HEADERS.items():
            self.send_header(key, value)
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def send_html(self, status_code: int, html: str):
        data = html.encode("utf-8")
        self.send_response(status_code)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.send_header("Cache-Control", "no-store")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


def main():
    ensure_data_root()
    server = ThreadingHTTPServer((HOST, PORT), RuleHandler)
    print(f"Rule server listening on {HOST}:{PORT}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()

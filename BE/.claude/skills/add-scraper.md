# /add-scraper

Add a new news source scraper to BoanCurator.

## Usage
```
/add-scraper <URL>
```

## What this skill does

Given a website URL, this skill:

1. **Fetches the URL** and analyzes the site structure
2. **Detects RSS feed** — checks for direct RSS or `<link rel="alternate" type="application/rss+xml">` in HTML
3. **Determines scraper type:**
   - **RSS found** → Register as `RSSGenericScraper` via the Sources API
   - **No RSS** → Generate a site-specific scraper (sser) by analyzing HTML structure

## Steps to follow

### Step 1: Fetch and analyze the URL
```bash
curl -sL "<URL>" | head -200
```

### Step 2: Check for RSS feed
Look for:
- Direct RSS/Atom XML content (`<rss>`, `<feed>`, `<channel>`)
- HTML `<link>` tags with `type="application/rss+xml"` or `type="application/atom+xml"`

### Step 3A: RSS found → Register via API
If RSS feed is detected:
1. Test the feed: read `BE/api/v1/endpoints/sources.py` for the `_detect_rss_feed()` logic
2. Suggest registering via the Sources API:
   ```
   POST /v1/sources
   {"url": "<feed_url>", "source_name": "<site_name>"}
   ```
3. Or add directly to `BE/scrapers/registry.py` in `get_system_scrapers()` if it should be a permanent source:
   ```python
   RSSGenericScraper(
       url="<feed_url>",
       period=10800,
       source_name="<Site Name>",
       has_full_content=True,  # or False with content_selector
   ),
   ```

### Step 3B: No RSS → Generate site-specific scraper
If no RSS feed exists:

1. **Analyze the article list page** — find the pattern of article links (CSS selector for article URLs)
2. **Analyze a single article page** — find the content body (CSS selector for article text)
3. **Generate scraper code** in `BE/scrapers/sser/<SiteName>Scraper.py`:

```python
import feedparser
from sqlmodel import Session
from ..BaseScraper import BaseScraper
from db.models import Article
from db import services

class <SiteName>Scraper(BaseScraper):
    def __init__(self, url: str, period: int = 10800):
        self.url = url
        self.period = period
        self.source_name = "<Site Name>"

    def collect(self, session: Session) -> list[Article]:
        articles = []
        # Implement site-specific collection logic
        # 1. Fetch article list page
        # 2. Extract article URLs
        # 3. For each URL, fetch and parse content
        # 4. Return Article objects
        return articles
```

4. **Add to registry** in `BE/scrapers/sser/__init__.py` and `BE/scrapers/registry.py`
5. **Generate test** in `BE/tests/test_<sitename>_scraper.py`
6. **Run test** to verify collection works

### Step 4: Verify
- Run the test to confirm articles are collected
- Check that `title`, `content`, `published_at`, and `image_urls` are properly extracted
- Verify content passes `_is_valid_content()` (minimum 50 chars)

## Key files to reference
- `BE/scrapers/BaseScraper.py` — Base class with shared utilities
- `BE/scrapers/rsser/RSSGenericScraper.py` — Generic RSS scraper
- `BE/scrapers/sser/BoanNewsScraper.py` — Example site-specific scraper
- `BE/scrapers/registry.py` — Scraper registration
- `BE/api/v1/endpoints/sources.py` — Sources API with RSS detection

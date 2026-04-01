# 보안 뉴스 수집 대상 도메인

## 수집 전략
- **RSS 우선**: RSS 피드가 있는 사이트는 feedparser 기반으로 수집 (안정적, 구현 빠름)
- **스크래핑**: RSS 미지원 사이트는 사이트별 파싱 로직 구현
- **전문 포함 여부**: RSS에 본문이 포함되면 추가 스크래핑 불필요, 요약만 있으면 _scrap_body 구현 필요

---

## 구현 완료

| 이름 | 수집 방식 | 난이도 | 언어 | RSS/URL |
|------|----------|--------|------|---------|
| 보안뉴스 | RSS + 스크래핑 | 중 | KR | http://www.boannews.com/media/news_rss.xml?skind=1 |
| GeekNews | RSS + 스크래핑 | 중 | KR | https://news.hada.io/rss/news |
| S2W | RSS (전문 포함) | 중 | EN | https://medium.com/feed/s2wblog |

---

## RSS 수집 가능 (구현 예정)

| 이름 | 난이도 | 언어 | RSS URL | 비고 |
|------|--------|------|---------|------|
| AhnLab ASEC | 중~상 | KR | https://asec.ahnlab.com/ko/feed/ | 악성코드 분석, 취약점, 위협 동향 |
| The Hacker News | 중 | EN | https://feeds.feedburner.com/TheHackersNews | 피드 정상 확인 (2026-03 최신) |
| Dark Reading | 중~상 | EN | https://www.darkreading.com/rss.xml | 피드 정상 확인 (2026-03 최신) |
| Krebs on Security | 상 | EN | https://krebsonsecurity.com/feed/ | 전문 포함 확인, 심층 조사 보도 |
| BleepingComputer | 중 | EN | https://www.bleepingcomputer.com/feed/ | 보안 뉴스 전반, 랜섬웨어 강점 |
| SecurityWeek | 중 | EN | https://www.securityweek.com/feed/ | 기업 보안, 취약점 뉴스 |
| 데일리시큐 | 중 | KR | https://www.dailysecu.com/rss/allArticle.xml | 국내 보안 뉴스, 보안뉴스와 양대 매체 |
| Schneier on Security | 상 | EN | https://www.schneier.com/feed/ | 보안 전문가 Bruce Schneier 블로그 |
| CISA Alerts | 중~상 | EN | https://www.cisa.gov/cybersecurity-advisories/all.xml | 미국 CISA 공식 보안 권고 |
| Mandiant (Google) | 상 | EN | https://cloud.google.com/blog/topics/threat-intelligence/rss/ | APT 분석, 위협 인텔리전스 |
| Unit 42 (Palo Alto) | 상 | EN | https://unit42.paloaltonetworks.com/feed/ | 위협 리서치, 악성코드 분석 |

---

## 스크래핑 필요 (구현 예정)

| 이름 | 난이도 | 언어 | URL | 비고 |
|------|--------|------|-----|------|
| ProjectDiscovery | 상 | EN | https://projectdiscovery.io/blog/ | RSS 없음, Next.js SPA (Playwright 필요할 수 있음) |
| Google Project Zero | 상 | EN | https://projectzero.google/ | RSS 없음, 정적 블로그 |
| 스틸리언 | 중 | KR | https://ufo.stealien.com/ | RSS 없음, CTI/R&D 카테고리 |
| KISA 보호나라 | 중 | KR | https://www.boho.or.kr/ | 보안 공지, 취약점 정보 |

---

## 제외 (사유)

| 이름 | 사유 |
|------|------|
| Threatpost | 2022년 이후 업데이트 중단 |
| Motherboard (VICE) | 보안 전문 매체가 아닌 일반 테크로 변질 |
| SK ShieldUS Rookies | 뉴스가 아닌 교육/훈련 시스템 페이지 |
| ZDNet Security | 보안 전용 섹션 약화, 접근 불안정 |
| Wired Security | 보안 전문성 낮음, 접근 불안정 |
| CyberScoop | RSS 미지원, 뉴스레터만 제공 |
| SOC Prime | RSS 미지원, AJAX 동적 로딩, 마케팅성 콘텐츠 비중 높음 |
| Talos Intelligence (취약점 목록) | 403 봇 차단, 취약점 DB 형식이라 기사 수집에 부적합 |

---

## 참고
- **Palo Alto Networks**: 기존 domains.md에서 Talos(Cisco)로 잘못 연결되어 있었음. Palo Alto의 위협 리서치는 Unit 42 (unit42.paloaltonetworks.com)가 정확함
- **금보원**: 공식 URL 미확인 상태, 확인 후 추가 예정
- **HackerOne Hacktivity**: 버그바운티 리포트 목록으로, 뉴스 수집보다는 취약점 DB 연동에 적합. 추후 별도 검토

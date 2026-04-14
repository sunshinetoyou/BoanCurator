FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

# Claude CLI 설치 — 바이너리는 /root/.local/bin/claude 로 설치되고
# ~/.claude/ 는 캐시용이라 런타임 호스트 마운트와 충돌 없음
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/root/.local/bin:${PATH}"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ARG BUILD_TAG=local
ARG BUILD_TIME=unknown
ENV BUILD_TAG=${BUILD_TAG}
ENV BUILD_TIME=${BUILD_TIME}
ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "11278"]

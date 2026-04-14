FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

# Claude CLI 설치 (~/.claude 는 런타임에 호스트 auth 디렉토리로 마운트되므로
# 설치 결과물을 /opt/claude 로 옮겨 PATH 에 등록)
RUN curl -fsSL https://claude.ai/install.sh | bash \
    && mv /root/.claude /opt/claude
ENV PATH="/opt/claude/bin:${PATH}"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ARG BUILD_TAG=local
ARG BUILD_TIME=unknown
ENV BUILD_TAG=${BUILD_TAG}
ENV BUILD_TIME=${BUILD_TIME}
ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "11278"]

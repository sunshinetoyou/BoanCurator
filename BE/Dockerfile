FROM python:3.11-slim

WORKDIR /app

RUN apt-get update && apt-get install -y \
    build-essential curl \
    && rm -rf /var/lib/apt/lists/*

# Claude CLI 설치
RUN curl -fsSL https://claude.ai/install.sh | bash
ENV PATH="/root/.claude/bin:${PATH}"

COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

ARG BUILD_TAG=local
ARG BUILD_TIME=unknown
ENV BUILD_TAG=${BUILD_TAG}
ENV BUILD_TIME=${BUILD_TIME}
ENV PYTHONUNBUFFERED=1

CMD ["uvicorn", "api.main:app", "--host", "0.0.0.0", "--port", "11278"]

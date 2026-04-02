import logging
import os
from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import JSONResponse

from api.v1.api import api_router
from db.connection import init_db

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)

app = FastAPI(title="News-Reader API")


@app.on_event("startup")
def on_startup():
    init_db()

origins = [
    "http://news.danyeon.cloud",
    "https://news.danyeon.cloud",
]

app.add_middleware(
    CORSMiddleware,
    allow_origins=origins,
    allow_origin_regex=r"^https?://(localhost|127\.0\.0\.1)(:\d+)?$",
    allow_credentials=True,
    allow_methods=["POST", "GET", "DELETE"],
    allow_headers=["*"],
    max_age=100,
)

BUILD_TAG = os.getenv("BUILD_TAG", "local")[:7]
BUILD_TIME = os.getenv("BUILD_TIME", "unknown")


@app.get("/health")
def health():
    return {"status": "ok", "tag": BUILD_TAG, "built_at": BUILD_TIME}


app.include_router(api_router, prefix="/v1")


@app.exception_handler(Exception)
async def global_exception_handler(request: Request, exc: Exception):
    logging.getLogger(__name__).error(f"Unhandled error: {exc}", exc_info=True)
    return JSONResponse(
        status_code=500,
        content={"detail": "Internal server error"},
    )

from fastapi import APIRouter
from api.v1.endpoints import auth, cardnews, searching, search, bookmarks, recommendations, users, sources

api_router = APIRouter()

api_router.include_router(auth.router, tags=["Auth"])
api_router.include_router(cardnews.router, tags=["Card News"])
api_router.include_router(searching.router, tags=["Theme Search"])
api_router.include_router(search.router, tags=["Semantic Search"])
api_router.include_router(users.router, tags=["Users"])
api_router.include_router(bookmarks.router, tags=["Bookmarks"])
api_router.include_router(recommendations.router, tags=["Recommendations"])
api_router.include_router(sources.router, tags=["Sources"])

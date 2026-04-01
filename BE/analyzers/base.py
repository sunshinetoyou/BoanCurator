from abc import ABC, abstractmethod
from db.models import Article, AnalysisData


class BaseAnalyzer(ABC):
    @abstractmethod
    def analyze(self, article: Article) -> AnalysisData:
        """입력 받은 자료를 LLM이 분석한 결과를 반환합니다."""
        pass

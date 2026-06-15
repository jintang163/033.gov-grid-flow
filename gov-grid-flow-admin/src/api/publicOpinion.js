import request from '@/utils/request'

export function getOpinionDashboard(data) {
  return request({
    url: '/public-opinion/dashboard',
    method: 'post',
    data
  })
}

export function getGridOpinion(gridId, data) {
  return request({
    url: `/public-opinion/grid/${gridId}`,
    method: 'post',
    data
  })
}

export function analyzeSentiment(text) {
  return request({
    url: '/public-opinion/sentiment/analyze',
    method: 'post',
    data: { text }
  })
}

export function batchAnalyzeSentiment(texts) {
  return request({
    url: '/public-opinion/sentiment/batch',
    method: 'post',
    data: texts
  })
}

export function getWordCloud(texts, topN = 50) {
  return request({
    url: '/public-opinion/wordcloud',
    method: 'post',
    data: { texts, topN }
  })
}

export function reprocessAllEvaluations() {
  return request({
    url: '/public-opinion/reprocess-all',
    method: 'post'
  })
}

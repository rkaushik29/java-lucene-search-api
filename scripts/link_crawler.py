import requests
from bs4 import BeautifulSoup

def crawl_link(url):
    response = requests.get(url)
    soup = BeautifulSoup(response.content, 'html.parser')
    text = soup.get_text()
    return text

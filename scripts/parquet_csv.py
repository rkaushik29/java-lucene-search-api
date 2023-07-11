import pandas as pd
df = pd.read_parquet('/Users/rohitkaushik/dev/tugraz/java-lucene-search-api/search-api/src/main/resources/websites-graz.parquet')
df.to_csv('search_parquet.csv')
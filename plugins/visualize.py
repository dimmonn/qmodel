import json
import numpy
import networkx as nx
import matplotlib.pyplot as plt

# Load the JSON file
with open('/Users/dima/IdeaProjects/qmodel/facebook_react.json', 'r') as file:
  graph_data = json.load(file)

# Create a NetworkX graph
G = nx.DiGraph()

# Add nodes
for node in graph_data['nodes']:
  G.add_node(
      node['id'],
      title=node.get('title', ''),
      subtitle=node.get('subTitle', ''),
      timestamp=node.get('timestamp', ''),
  )

# Add edges
for edge in graph_data['edges']:
  G.add_edge(edge['source'], edge['target'], main_stat=edge.get('mainStat', ''))

# Visualize the graph
plt.figure(figsize=(15, 10))
pos = nx.spring_layout(G)  # Choose a layout
nx.draw(
    G,
    pos,
    node_size=20,
    node_color='blue',
    edge_color='gray',
    with_labels=False,
    alpha=0.7,
)
plt.title("Commit Graph")
plt.show()
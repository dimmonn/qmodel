import networkx as nx
import matplotlib.pyplot as plt

# Create a directed graph
G = nx.DiGraph()

# Add nodes
G.add_nodes_from(["A", "B", "C", "D", "E", "F"])

# Add edges
G.add_edges_from([("A", "B"), ("A", "C"), ("B", "D"), ("C", "E"), ("D", "F"), ("E", "F")])

# Define positions for each node
pos = {
    "A": (0, 2),
    "B": (1, 3),
    "C": (1, 1),
    "D": (2, 3),
    "E": (2, 1),
    "F": (3, 2),
}

# Draw the graph
plt.figure(figsize=(8, 6))
nx.draw(G, pos, with_labels=True, node_size=2000, node_color='skyblue', font_size=15, font_weight='bold', arrowsize=20)

# Draw edge labels
edge_labels = {("A", "B"): "main", ("A", "C"): "feature-1", ("B", "D"): "main", ("C", "E"): "feature-1", ("D", "F"): "main", ("E", "F"): "feature-1"}
nx.draw_networkx_edge_labels(G, pos, edge_labels=edge_labels, font_color='red', font_size=12)

# Add annotations
plt.text(-0.5, 2.2, "Number of vertices: 6", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))
plt.text(-0.5, 1.9, "Number of edges: 6", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))
plt.text(-0.5, 1.6, "Branches: main, feature-1", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))
plt.text(-0.5, 1.3, "Average degree: 1", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))
plt.text(-0.5, 1.0, "Maximum degree: 2", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))
plt.text(-0.5, 0.7, "Depth of commit history: 3", fontsize=12, bbox=dict(facecolor='white', alpha=0.5))

plt.title("Commit Graph Visualization")
plt.show()

-- MySQL dump 10.13  Distrib 9.6.0, for macos26.4 (arm64)
--
-- Host: 127.0.0.1    Database: qmodel_demo
-- ------------------------------------------------------
-- Server version	8.0.44

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `action`
--

DROP TABLE IF EXISTS `action`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `action` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `completed_at` datetime(6) DEFAULT NULL,
  `description` longtext,
  `events` longtext,
  `failed` int NOT NULL,
  `failed_percent` double NOT NULL,
  `name` varchar(255) DEFAULT NULL,
  `other` int NOT NULL,
  `other_percent` double NOT NULL,
  `passed` int NOT NULL,
  `passed_percent` double NOT NULL,
  `raw_check_runs` longtext,
  `result` varchar(255) DEFAULT NULL,
  `started_at` datetime(6) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `summary` varchar(255) DEFAULT NULL,
  `text` longtext,
  `title` varchar(255) DEFAULT NULL,
  `total` int NOT NULL,
  `commit_sha` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKkas1rhv6dgpjwnrysq081pw1d` (`commit_sha`),
  CONSTRAINT `FKkas1rhv6dgpjwnrysq081pw1d` FOREIGN KEY (`commit_sha`) REFERENCES `commit` (`sha`)
) ENGINE=InnoDB AUTO_INCREMENT=446176 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `agraph`
--

DROP TABLE IF EXISTS `agraph`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `agraph` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `graph` longtext,
  `project_project_name` varchar(255) DEFAULT NULL,
  `project_project_owner` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKnbqtvngiuy5qou6bnmdp69pq1` (`project_project_name`,`project_project_owner`),
  CONSTRAINT `FKnbqtvngiuy5qou6bnmdp69pq1` FOREIGN KEY (`project_project_name`, `project_project_owner`) REFERENCES `project` (`project_name`, `project_owner`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit`
--

DROP TABLE IF EXISTS `commit`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commit` (
  `sha` varchar(255) NOT NULL,
  `author` varchar(255) DEFAULT NULL,
  `average_degree` double DEFAULT NULL,
  `comment_count` int DEFAULT NULL,
  `commit_date` datetime(6) DEFAULT NULL,
  `email` varchar(255) DEFAULT NULL,
  `in_degree` int DEFAULT NULL,
  `is_merge` bit(1) DEFAULT NULL,
  `max_depth_of_commit_history` int DEFAULT NULL,
  `merge_count` int DEFAULT NULL,
  `message` longtext,
  `min_depth_of_commit_history` int DEFAULT NULL,
  `num_of_files_changed` int DEFAULT NULL,
  `number_of_branches` int DEFAULT NULL,
  `number_of_edges` int DEFAULT NULL,
  `number_of_vertices` int DEFAULT NULL,
  `out_degree` int DEFAULT NULL,
  `project_name` varchar(255) DEFAULT NULL,
  `project_owner` varchar(255) DEFAULT NULL,
  `raw_data` longtext,
  `state` varchar(255) DEFAULT NULL,
  `a_graph_id` bigint DEFAULT NULL,
  `days_since_last_merge_on_segment` int DEFAULT NULL,
  `distance_to_branch_start` int DEFAULT NULL,
  `upstream_heads_unique_on_segment` int DEFAULT NULL,
  `actions_id` bigint DEFAULT NULL,
  `issue_id` bigint DEFAULT NULL,
  `issue_project_name` varchar(255) DEFAULT NULL,
  `issue_project_owner` varchar(255) DEFAULT NULL,
  `pr_id` bigint DEFAULT NULL,
  `pr_project_name` varchar(100) DEFAULT NULL,
  `pr_project_owner` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`sha`),
  KEY `FKls6wbtxsw5qio8sdon6shu19m` (`a_graph_id`),
  KEY `FK7gjvl8qgwrdrcgpqb5qckkqm6` (`actions_id`),
  KEY `FK4sq4iqd0x5743htisb7iiuq2h` (`issue_id`,`issue_project_name`,`issue_project_owner`),
  KEY `FKmbhd61d8utukr6qja791uqqfu` (`pr_id`,`pr_project_name`,`pr_project_owner`),
  CONSTRAINT `FK4sq4iqd0x5743htisb7iiuq2h` FOREIGN KEY (`issue_id`, `issue_project_name`, `issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKls6wbtxsw5qio8sdon6shu19m` FOREIGN KEY (`a_graph_id`) REFERENCES `agraph` (`id`),
  CONSTRAINT `FKmbhd61d8utukr6qja791uqqfu` FOREIGN KEY (`pr_id`, `pr_project_name`, `pr_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit_branches`
--

DROP TABLE IF EXISTS `commit_branches`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commit_branches` (
  `commit_sha` varchar(255) NOT NULL,
  `branches` varchar(255) DEFAULT NULL,
  KEY `FKej915chup3aed5buxpaxg5wxd` (`commit_sha`),
  CONSTRAINT `FKej915chup3aed5buxpaxg5wxd` FOREIGN KEY (`commit_sha`) REFERENCES `commit` (`sha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit_file_changes`
--

DROP TABLE IF EXISTS `commit_file_changes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commit_file_changes` (
  `commit_sha` varchar(255) NOT NULL,
  `file_changes_id` bigint NOT NULL,
  KEY `FKsbqgakteolfactrf9j5lngr2k` (`file_changes_id`),
  KEY `FKc7ajxgujrtt0vneb1fquyvosr` (`commit_sha`),
  CONSTRAINT `FKc7ajxgujrtt0vneb1fquyvosr` FOREIGN KEY (`commit_sha`) REFERENCES `commit` (`sha`),
  CONSTRAINT `FKsbqgakteolfactrf9j5lngr2k` FOREIGN KEY (`file_changes_id`) REFERENCES `file_change` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `commit_sub_graph_nodes`
--

DROP TABLE IF EXISTS `commit_sub_graph_nodes`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `commit_sub_graph_nodes` (
  `commit_sha` varchar(255) NOT NULL,
  `sub_graph_nodes` varchar(255) DEFAULT NULL,
  KEY `FKlp85laxchopr2cbt5nju04xfn` (`commit_sha`),
  CONSTRAINT `FKlp85laxchopr2cbt5nju04xfn` FOREIGN KEY (`commit_sha`) REFERENCES `commit` (`sha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_change`
--

DROP TABLE IF EXISTS `file_change`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_change` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `change_date` datetime(6) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `patch` longtext,
  `raw_data` longtext,
  `sha` varchar(255) DEFAULT NULL,
  `status` varchar(255) DEFAULT NULL,
  `total_additions` int DEFAULT NULL,
  `total_changes` int DEFAULT NULL,
  `total_deletions` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=1019401 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `file_change_changed_lines`
--

DROP TABLE IF EXISTS `file_change_changed_lines`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `file_change_changed_lines` (
  `file_change_id` bigint NOT NULL,
  `changed_lines` int DEFAULT NULL,
  KEY `FK2p9few9q2dhrtdf47ubj7vjh0` (`file_change_id`),
  CONSTRAINT `FK2p9few9q2dhrtdf47ubj7vjh0` FOREIGN KEY (`file_change_id`) REFERENCES `file_change` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project`
--

DROP TABLE IF EXISTS `project`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project` (
  `project_name` varchar(255) NOT NULL,
  `project_owner` varchar(255) NOT NULL,
  PRIMARY KEY (`project_name`,`project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue`
--

DROP TABLE IF EXISTS `project_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue` (
  `id` bigint NOT NULL,
  `project_name` varchar(255) NOT NULL,
  `project_owner` varchar(255) NOT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `fix_pr` bigint DEFAULT NULL,
  `merged_at` datetime(6) DEFAULT NULL,
  `raw_issue` longtext,
  `state` varchar(255) DEFAULT NULL,
  `title` longtext,
  `updated_at` datetime(6) DEFAULT NULL,
  `project_project_name` varchar(255) NOT NULL,
  `project_project_owner` varchar(255) NOT NULL,
  `fixpr_id` bigint DEFAULT NULL,
  `fixpr_project_name` varchar(100) DEFAULT NULL,
  `fixpr_project_owner` varchar(100) DEFAULT NULL,
  `reaction_id` bigint DEFAULT NULL,
  PRIMARY KEY (`id`,`project_name`,`project_owner`),
  KEY `FKl3ammavi4qmxgwuisurla7iin` (`project_project_name`,`project_project_owner`),
  KEY `FKkak5ymm7ouq2xj871ldr9oknr` (`fixpr_id`,`fixpr_project_name`,`fixpr_project_owner`),
  KEY `FK87m8rxrxo6shxsavoc3ac3s52` (`reaction_id`),
  CONSTRAINT `FK87m8rxrxo6shxsavoc3ac3s52` FOREIGN KEY (`reaction_id`) REFERENCES `reaction` (`id`),
  CONSTRAINT `FKkak5ymm7ouq2xj871ldr9oknr` FOREIGN KEY (`fixpr_id`, `fixpr_project_name`, `fixpr_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKl3ammavi4qmxgwuisurla7iin` FOREIGN KEY (`project_project_name`, `project_project_owner`) REFERENCES `project` (`project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_assignees`
--

DROP TABLE IF EXISTS `project_issue_assignees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_assignees` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `assignees` varchar(255) DEFAULT NULL,
  KEY `FK31p7gplml6dcrx4m59ut78ncf` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  CONSTRAINT `FK31p7gplml6dcrx4m59ut78ncf` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_bug_introducing_commits`
--

DROP TABLE IF EXISTS `project_issue_bug_introducing_commits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_bug_introducing_commits` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `bug_introducing_commits_sha` varchar(255) NOT NULL,
  KEY `FKqgsi7hkl37r70b0g8vtohx6r7` (`bug_introducing_commits_sha`),
  KEY `FK3ymunxfcy3jfmfkx5a0077hwc` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  CONSTRAINT `FK3ymunxfcy3jfmfkx5a0077hwc` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKqgsi7hkl37r70b0g8vtohx6r7` FOREIGN KEY (`bug_introducing_commits_sha`) REFERENCES `commit` (`sha`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_fixing_commits`
--

DROP TABLE IF EXISTS `project_issue_fixing_commits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_fixing_commits` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `fixing_commits_sha` varchar(255) NOT NULL,
  KEY `FKiqtoddfjjd3w6pdwx559y3bir` (`fixing_commits_sha`),
  KEY `FKjma6ojw0ifh8yna9hucubfn6j` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  CONSTRAINT `FKiqtoddfjjd3w6pdwx559y3bir` FOREIGN KEY (`fixing_commits_sha`) REFERENCES `commit` (`sha`),
  CONSTRAINT `FKjma6ojw0ifh8yna9hucubfn6j` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_labels`
--

DROP TABLE IF EXISTS `project_issue_labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_labels` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `labels` varchar(255) DEFAULT NULL,
  KEY `FK51yffd9vtvvxx0u3rsvi7mlgf` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  CONSTRAINT `FK51yffd9vtvvxx0u3rsvi7mlgf` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_project_pull`
--

DROP TABLE IF EXISTS `project_issue_project_pull`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_project_pull` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  PRIMARY KEY (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`,`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  KEY `FKdr8tlcb7fwo78ifcche7k4u3w` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKamig6v82mqgfhsakajqyd9mbr` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKdr8tlcb7fwo78ifcche7k4u3w` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_issue_time_line`
--

DROP TABLE IF EXISTS `project_issue_time_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_issue_time_line` (
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  `time_line_id` bigint NOT NULL,
  PRIMARY KEY (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`,`time_line_id`),
  UNIQUE KEY `UK_o4nok7p6df7kiu4b89rlr4y6w` (`time_line_id`),
  CONSTRAINT `FK942le3ixv2y5k2vfe33c6uteu` FOREIGN KEY (`time_line_id`) REFERENCES `timeline` (`id`),
  CONSTRAINT `FKjh4it9jrtvsry5dvyi7bhka97` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull`
--

DROP TABLE IF EXISTS `project_pull`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull` (
  `id` bigint NOT NULL,
  `project_name` varchar(100) NOT NULL,
  `project_owner` varchar(100) NOT NULL,
  `closed_at` datetime(6) DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  `is_bug_fix` bit(1) DEFAULT NULL,
  `merged_at` datetime(6) DEFAULT NULL,
  `raw_pull` longtext,
  `state` varchar(255) DEFAULT NULL,
  `title` longtext,
  `updated_at` datetime(6) DEFAULT NULL,
  `project_project_name` varchar(255) DEFAULT NULL,
  `project_project_owner` varchar(255) DEFAULT NULL,
  `reaction_id` bigint DEFAULT NULL,
  `commit_sha` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`,`project_name`,`project_owner`),
  KEY `FKr3310ggthu8exu7gvamy369` (`project_project_name`,`project_project_owner`),
  KEY `FKkrlpgnlppoxc5xn6b2u7c3ktu` (`reaction_id`),
  KEY `FK8fmfdf689fjganvgv03tr7l6n` (`commit_sha`),
  CONSTRAINT `FK8fmfdf689fjganvgv03tr7l6n` FOREIGN KEY (`commit_sha`) REFERENCES `commit` (`sha`),
  CONSTRAINT `FKkrlpgnlppoxc5xn6b2u7c3ktu` FOREIGN KEY (`reaction_id`) REFERENCES `reaction` (`id`),
  CONSTRAINT `FKr3310ggthu8exu7gvamy369` FOREIGN KEY (`project_project_name`, `project_project_owner`) REFERENCES `project` (`project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_assignees`
--

DROP TABLE IF EXISTS `project_pull_assignees`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_assignees` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `assignees` varchar(255) DEFAULT NULL,
  KEY `FKpa4od9hsxi4g94rn4vjr6tdxs` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKpa4od9hsxi4g94rn4vjr6tdxs` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_commits`
--

DROP TABLE IF EXISTS `project_pull_commits`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_commits` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `commits_sha` varchar(255) NOT NULL,
  UNIQUE KEY `UK_el0saggvnsrn4qbn04aasvox9` (`commits_sha`),
  KEY `FKkve8eodeejtqx6sj6l5002k1k` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKch96l4eeribmgd8y6yf92t7i` FOREIGN KEY (`commits_sha`) REFERENCES `commit` (`sha`),
  CONSTRAINT `FKkve8eodeejtqx6sj6l5002k1k` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_labels`
--

DROP TABLE IF EXISTS `project_pull_labels`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_labels` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `labels` varchar(255) DEFAULT NULL,
  KEY `FKq03b8g5ybpnuu18v72pp7bdwg` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKq03b8g5ybpnuu18v72pp7bdwg` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_project_issue`
--

DROP TABLE IF EXISTS `project_pull_project_issue`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_project_issue` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `project_issue_id` bigint NOT NULL,
  `project_issue_project_name` varchar(255) NOT NULL,
  `project_issue_project_owner` varchar(255) NOT NULL,
  PRIMARY KEY (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`,`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  KEY `FK4vjrfaih3h1gb0rkg6aa3e9t0` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  CONSTRAINT `FK4vjrfaih3h1gb0rkg6aa3e9t0` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKc8o48jium1keexegthowad610` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_project_issues`
--

DROP TABLE IF EXISTS `project_pull_project_issues`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_project_issues` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `project_issues_id` bigint NOT NULL,
  `project_issues_project_name` varchar(255) NOT NULL,
  `project_issues_project_owner` varchar(255) NOT NULL,
  PRIMARY KEY (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`,`project_issues_id`,`project_issues_project_name`,`project_issues_project_owner`),
  UNIQUE KEY `UK_n3fovvysfso9su3dsrmy9o3m6` (`project_issues_id`,`project_issues_project_name`,`project_issues_project_owner`),
  CONSTRAINT `FKcd076gt3r6yklmr07qurj80c7` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKo0l0iri7f46cencb1mp3gqgxe` FOREIGN KEY (`project_issues_id`, `project_issues_project_name`, `project_issues_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_reviewers`
--

DROP TABLE IF EXISTS `project_pull_reviewers`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_reviewers` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `reviewers` varchar(255) DEFAULT NULL,
  KEY `FKf10mqmh9bflb4jw04g2rd6aso` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKf10mqmh9bflb4jw04g2rd6aso` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `project_pull_time_line`
--

DROP TABLE IF EXISTS `project_pull_time_line`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `project_pull_time_line` (
  `project_pull_id` bigint NOT NULL,
  `project_pull_project_name` varchar(100) NOT NULL,
  `project_pull_project_owner` varchar(100) NOT NULL,
  `time_line_id` bigint NOT NULL,
  PRIMARY KEY (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`,`time_line_id`),
  UNIQUE KEY `UK_nise2jp3jfix0qfk76wstgp8` (`time_line_id`),
  CONSTRAINT `FK88d5hfa4xvd120ry9enmdigax` FOREIGN KEY (`time_line_id`) REFERENCES `timeline` (`id`),
  CONSTRAINT `FKnfg3p8sb1r03uogoe2ty4y21x` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `reaction`
--

DROP TABLE IF EXISTS `reaction`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reaction` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `confused` int NOT NULL,
  `eyes` int NOT NULL,
  `heart` int NOT NULL,
  `hooray` int NOT NULL,
  `laugh` int NOT NULL,
  `minus_one` int NOT NULL,
  `plus_one` int NOT NULL,
  `rocket` int NOT NULL,
  `total_count` int NOT NULL,
  `url` varchar(255) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6333 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Temporary view structure for view `rq1_issue_bic_graph_churn`
--

DROP TABLE IF EXISTS `rq1_issue_bic_graph_churn`;
/*!50001 DROP VIEW IF EXISTS `rq1_issue_bic_graph_churn`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq1_issue_bic_graph_churn` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `issue_id`,
 1 AS `issue_resolution_hours`,
 1 AS `bic_num_commits`,
 1 AS `bic_avg_min_depth`,
 1 AS `bic_avg_max_depth`,
 1 AS `bic_avg_fp_distance`,
 1 AS `bic_max_fp_distance`,
 1 AS `bic_avg_upstream_heads`,
 1 AS `bic_max_upstream_heads`,
 1 AS `bic_avg_days_since_merge`,
 1 AS `bic_max_days_since_merge`,
 1 AS `bic_avg_in_degree`,
 1 AS `bic_avg_out_degree`,
 1 AS `bic_avg_branches`,
 1 AS `bic_avg_average_degree`,
 1 AS `bic_total_additions`,
 1 AS `bic_total_deletions`,
 1 AS `bic_total_changes`,
 1 AS `bic_avg_changes_per_file`,
 1 AS `bic_max_changes_in_file`,
 1 AS `bic_num_files_changed`,
 1 AS `bic_change_density_per_file`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `rq1_issue_fix_graph_churn`
--

DROP TABLE IF EXISTS `rq1_issue_fix_graph_churn`;
/*!50001 DROP VIEW IF EXISTS `rq1_issue_fix_graph_churn`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq1_issue_fix_graph_churn` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `issue_id`,
 1 AS `issue_resolution_hours`,
 1 AS `fix_num_commits`,
 1 AS `fix_avg_min_depth`,
 1 AS `fix_avg_max_depth`,
 1 AS `fix_avg_fp_distance`,
 1 AS `fix_max_fp_distance`,
 1 AS `fix_avg_upstream_heads`,
 1 AS `fix_max_upstream_heads`,
 1 AS `fix_avg_days_since_merge`,
 1 AS `fix_max_days_since_merge`,
 1 AS `fix_avg_in_degree`,
 1 AS `fix_avg_out_degree`,
 1 AS `fix_avg_branches`,
 1 AS `fix_avg_average_degree`,
 1 AS `fix_total_additions`,
 1 AS `fix_total_deletions`,
 1 AS `fix_total_changes`,
 1 AS `fix_avg_changes_per_file`,
 1 AS `fix_max_changes_in_file`,
 1 AS `fix_num_files_changed`,
 1 AS `fix_change_density_per_file`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `rq1_issue_graph_ci_metrics`
--

DROP TABLE IF EXISTS `rq1_issue_graph_ci_metrics`;
/*!50001 DROP VIEW IF EXISTS `rq1_issue_graph_ci_metrics`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq1_issue_graph_ci_metrics` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `issue_id`,
 1 AS `issue_resolution_hours`,
 1 AS `num_fix_commits`,
 1 AS `avg_min_depth`,
 1 AS `avg_max_depth`,
 1 AS `avg_fp_distance`,
 1 AS `max_fp_distance`,
 1 AS `avg_upstream_heads`,
 1 AS `avg_days_since_merge`,
 1 AS `avg_in_degree`,
 1 AS `avg_out_degree`,
 1 AS `avg_branches`,
 1 AS `avg_average_degree`,
 1 AS `avg_files_changed`,
 1 AS `total_ci_runs`,
 1 AS `total_ci_passed`,
 1 AS `total_ci_failed`,
 1 AS `total_ci_other`,
 1 AS `avg_ci_passed_percent`,
 1 AS `avg_ci_failed_percent`,
 1 AS `avg_ci_other_percent`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `rq2_pr_bic_graph_churn`
--

DROP TABLE IF EXISTS `rq2_pr_bic_graph_churn`;
/*!50001 DROP VIEW IF EXISTS `rq2_pr_bic_graph_churn`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq2_pr_bic_graph_churn` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `pr_id`,
 1 AS `pr_review_hours`,
 1 AS `bic_num_commits`,
 1 AS `bic_avg_min_depth`,
 1 AS `bic_avg_max_depth`,
 1 AS `bic_avg_fp_distance`,
 1 AS `bic_max_fp_distance`,
 1 AS `bic_avg_upstream_heads`,
 1 AS `bic_max_upstream_heads`,
 1 AS `bic_avg_days_since_merge`,
 1 AS `bic_max_days_since_merge`,
 1 AS `bic_avg_in_degree`,
 1 AS `bic_avg_out_degree`,
 1 AS `bic_avg_branches`,
 1 AS `bic_avg_average_degree`,
 1 AS `bic_total_additions`,
 1 AS `bic_total_deletions`,
 1 AS `bic_total_changes`,
 1 AS `bic_avg_changes_per_file`,
 1 AS `bic_max_changes_in_file`,
 1 AS `bic_num_files_changed`,
 1 AS `bic_change_density_per_file`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `rq2_pr_fix_graph_churn`
--

DROP TABLE IF EXISTS `rq2_pr_fix_graph_churn`;
/*!50001 DROP VIEW IF EXISTS `rq2_pr_fix_graph_churn`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq2_pr_fix_graph_churn` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `pr_id`,
 1 AS `pr_review_hours`,
 1 AS `fix_num_commits`,
 1 AS `fix_avg_min_depth`,
 1 AS `fix_avg_max_depth`,
 1 AS `fix_avg_fp_distance`,
 1 AS `fix_max_fp_distance`,
 1 AS `fix_avg_upstream_heads`,
 1 AS `fix_max_upstream_heads`,
 1 AS `fix_avg_days_since_merge`,
 1 AS `fix_max_days_since_merge`,
 1 AS `fix_avg_in_degree`,
 1 AS `fix_avg_out_degree`,
 1 AS `fix_avg_branches`,
 1 AS `fix_avg_average_degree`,
 1 AS `fix_total_additions`,
 1 AS `fix_total_deletions`,
 1 AS `fix_total_changes`,
 1 AS `fix_avg_changes_per_file`,
 1 AS `fix_max_changes_in_file`,
 1 AS `fix_num_files_changed`,
 1 AS `fix_change_density_per_file`*/;
SET character_set_client = @saved_cs_client;

--
-- Temporary view structure for view `rq3_issue_graph_churn`
--

DROP TABLE IF EXISTS `rq3_issue_graph_churn`;
/*!50001 DROP VIEW IF EXISTS `rq3_issue_graph_churn`*/;
SET @saved_cs_client     = @@character_set_client;
/*!50503 SET character_set_client = utf8mb4 */;
/*!50001 CREATE VIEW `rq3_issue_graph_churn` AS SELECT 
 1 AS `project_owner`,
 1 AS `project_name`,
 1 AS `issue_id`,
 1 AS `issue_resolution_hours`,
 1 AS `fix_num_commits`,
 1 AS `fix_avg_min_depth`,
 1 AS `fix_avg_max_depth`,
 1 AS `fix_avg_fp_distance`,
 1 AS `fix_max_fp_distance`,
 1 AS `fix_avg_upstream_heads`,
 1 AS `fix_max_upstream_heads`,
 1 AS `fix_avg_days_since_merge`,
 1 AS `fix_max_days_since_merge`,
 1 AS `fix_avg_in_degree`,
 1 AS `fix_avg_out_degree`,
 1 AS `fix_avg_branches`,
 1 AS `fix_avg_average_degree`,
 1 AS `fix_total_additions`,
 1 AS `fix_total_deletions`,
 1 AS `fix_total_changes`,
 1 AS `fix_avg_changes_per_file`,
 1 AS `fix_max_changes_in_file`,
 1 AS `fix_num_files_changed`,
 1 AS `fix_change_density_per_file`,
 1 AS `bic_num_commits`,
 1 AS `bic_avg_min_depth`,
 1 AS `bic_avg_max_depth`,
 1 AS `bic_avg_fp_distance`,
 1 AS `bic_max_fp_distance`,
 1 AS `bic_avg_upstream_heads`,
 1 AS `bic_max_upstream_heads`,
 1 AS `bic_avg_days_since_merge`,
 1 AS `bic_max_days_since_merge`,
 1 AS `bic_avg_in_degree`,
 1 AS `bic_avg_out_degree`,
 1 AS `bic_avg_branches`,
 1 AS `bic_avg_average_degree`,
 1 AS `bic_total_additions`,
 1 AS `bic_total_deletions`,
 1 AS `bic_total_changes`,
 1 AS `bic_avg_changes_per_file`,
 1 AS `bic_max_changes_in_file`,
 1 AS `bic_num_files_changed`,
 1 AS `bic_change_density_per_file`*/;
SET character_set_client = @saved_cs_client;

--
-- Table structure for table `timeline`
--

DROP TABLE IF EXISTS `timeline`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timeline` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `created_at` datetime(6) DEFAULT NULL,
  `message` longtext,
  `raw_data` longtext,
  `project_issue_id` bigint DEFAULT NULL,
  `project_issue_project_name` varchar(255) DEFAULT NULL,
  `project_issue_project_owner` varchar(255) DEFAULT NULL,
  `project_pull_id` bigint DEFAULT NULL,
  `project_pull_project_name` varchar(100) DEFAULT NULL,
  `project_pull_project_owner` varchar(100) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `FKqbqrgegu94tos6w97157okxl4` (`project_issue_id`,`project_issue_project_name`,`project_issue_project_owner`),
  KEY `FKpxbwjly301itpqs157y8yvn2j` (`project_pull_id`,`project_pull_project_name`,`project_pull_project_owner`),
  CONSTRAINT `FKpxbwjly301itpqs157y8yvn2j` FOREIGN KEY (`project_pull_id`, `project_pull_project_name`, `project_pull_project_owner`) REFERENCES `project_pull` (`id`, `project_name`, `project_owner`),
  CONSTRAINT `FKqbqrgegu94tos6w97157okxl4` FOREIGN KEY (`project_issue_id`, `project_issue_project_name`, `project_issue_project_owner`) REFERENCES `project_issue` (`id`, `project_name`, `project_owner`)
) ENGINE=InnoDB AUTO_INCREMENT=2217312 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Table structure for table `timeline_pull_ids`
--

DROP TABLE IF EXISTS `timeline_pull_ids`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `timeline_pull_ids` (
  `timeline_id` bigint NOT NULL,
  `pull_ids` bigint DEFAULT NULL,
  KEY `FKbq705kf93elcro0yjobgst16s` (`timeline_id`),
  CONSTRAINT `FKbq705kf93elcro0yjobgst16s` FOREIGN KEY (`timeline_id`) REFERENCES `timeline` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Final view structure for view `rq1_issue_bic_graph_churn`
--

/*!50001 DROP VIEW IF EXISTS `rq1_issue_bic_graph_churn`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq1_issue_bic_graph_churn` AS select `pi`.`project_owner` AS `project_owner`,`pi`.`project_name` AS `project_name`,`pi`.`id` AS `issue_id`,timestampdiff(HOUR,`pi`.`created_at`,`pi`.`closed_at`) AS `issue_resolution_hours`,count(distinct `pibic`.`bug_introducing_commits_sha`) AS `bic_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `bic_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `bic_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `bic_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `bic_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `bic_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `bic_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `bic_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `bic_max_days_since_merge`,avg(`c`.`in_degree`) AS `bic_avg_in_degree`,avg(`c`.`out_degree`) AS `bic_avg_out_degree`,avg(`c`.`number_of_branches`) AS `bic_avg_branches`,avg(`c`.`average_degree`) AS `bic_avg_average_degree`,sum(`cm`.`total_additions`) AS `bic_total_additions`,sum(`cm`.`total_deletions`) AS `bic_total_deletions`,sum(`cm`.`total_changes`) AS `bic_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `bic_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `bic_max_changes_in_file`,avg(`cm`.`num_files_changed`) AS `bic_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `bic_change_density_per_file` from (((`project_issue` `pi` join `project_issue_bug_introducing_commits` `pibic` on(((`pi`.`id` = `pibic`.`project_issue_id`) and (`pi`.`project_name` = `pibic`.`project_issue_project_name`) and (`pi`.`project_owner` = `pibic`.`project_issue_project_owner`)))) join `commit` `c` on((`c`.`sha` = `pibic`.`bug_introducing_commits_sha`))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pi`.`state` = 'closed') and (`pi`.`created_at` is not null) and (`pi`.`closed_at` is not null) and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0) and (`pi`.`project_owner` = 'ansible')) group by `pi`.`project_owner`,`pi`.`project_name`,`pi`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rq1_issue_fix_graph_churn`
--

/*!50001 DROP VIEW IF EXISTS `rq1_issue_fix_graph_churn`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq1_issue_fix_graph_churn` AS select `pi`.`project_owner` AS `project_owner`,`pi`.`project_name` AS `project_name`,`pi`.`id` AS `issue_id`,timestampdiff(HOUR,`pi`.`created_at`,`pi`.`closed_at`) AS `issue_resolution_hours`,count(distinct `pifc`.`fixing_commits_sha`) AS `fix_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `fix_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `fix_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `fix_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `fix_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `fix_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `fix_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `fix_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `fix_max_days_since_merge`,avg(`c`.`in_degree`) AS `fix_avg_in_degree`,avg(`c`.`out_degree`) AS `fix_avg_out_degree`,avg(`c`.`number_of_branches`) AS `fix_avg_branches`,avg(`c`.`average_degree`) AS `fix_avg_average_degree`,sum(`cm`.`total_additions`) AS `fix_total_additions`,sum(`cm`.`total_deletions`) AS `fix_total_deletions`,sum(`cm`.`total_changes`) AS `fix_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `fix_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `fix_max_changes_in_file`,avg(`cm`.`num_files_changed`) AS `fix_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `fix_change_density_per_file` from (((`project_issue` `pi` join `project_issue_fixing_commits` `pifc` on(((`pi`.`id` = `pifc`.`project_issue_id`) and (`pi`.`project_name` = `pifc`.`project_issue_project_name`) and (`pi`.`project_owner` = `pifc`.`project_issue_project_owner`)))) join `commit` `c` on((`c`.`sha` = `pifc`.`fixing_commits_sha`))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pi`.`state` = 'closed') and (`pi`.`created_at` is not null) and (`pi`.`closed_at` is not null) and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0) and (`pi`.`project_owner` = 'ansible')) group by `pi`.`project_owner`,`pi`.`project_name`,`pi`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rq1_issue_graph_ci_metrics`
--

/*!50001 DROP VIEW IF EXISTS `rq1_issue_graph_ci_metrics`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`admin`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq1_issue_graph_ci_metrics` AS select `pi`.`project_owner` AS `project_owner`,`pi`.`project_name` AS `project_name`,`pi`.`id` AS `issue_id`,timestampdiff(HOUR,`pi`.`created_at`,`pi`.`closed_at`) AS `issue_resolution_hours`,count(distinct `pifc`.`fixing_commits_sha`) AS `num_fix_commits`,avg(`c`.`min_depth_of_commit_history`) AS `avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `avg_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `avg_days_since_merge`,avg(`c`.`in_degree`) AS `avg_in_degree`,avg(`c`.`out_degree`) AS `avg_out_degree`,avg(`c`.`number_of_branches`) AS `avg_branches`,avg(`c`.`average_degree`) AS `avg_average_degree`,avg(`c`.`num_of_files_changed`) AS `avg_files_changed`,sum(`a`.`total`) AS `total_ci_runs`,sum(`a`.`passed`) AS `total_ci_passed`,sum(`a`.`failed`) AS `total_ci_failed`,sum(`a`.`other`) AS `total_ci_other`,avg(`a`.`passed_percent`) AS `avg_ci_passed_percent`,avg(`a`.`failed_percent`) AS `avg_ci_failed_percent`,avg(`a`.`other_percent`) AS `avg_ci_other_percent` from (((`project_issue` `pi` join `project_issue_fixing_commits` `pifc` on(((`pi`.`id` = `pifc`.`project_issue_id`) and (`pi`.`project_name` = `pifc`.`project_issue_project_name`) and (`pi`.`project_owner` = `pifc`.`project_issue_project_owner`)))) join `commit` `c` on((`c`.`sha` = `pifc`.`fixing_commits_sha`))) left join `action` `a` on((`a`.`commit_sha` = `c`.`sha`))) where ((`pi`.`state` = 'closed') and (`pi`.`created_at` is not null) and (`pi`.`closed_at` is not null) and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0)) group by `pi`.`project_owner`,`pi`.`project_name`,`pi`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rq2_pr_bic_graph_churn`
--

/*!50001 DROP VIEW IF EXISTS `rq2_pr_bic_graph_churn`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`root`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq2_pr_bic_graph_churn` AS select `pp`.`project_owner` AS `project_owner`,`pp`.`project_name` AS `project_name`,`pp`.`id` AS `pr_id`,timestampdiff(HOUR,`pp`.`created_at`,`pp`.`merged_at`) AS `pr_review_hours`,count(distinct `c`.`sha`) AS `bic_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `bic_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `bic_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `bic_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `bic_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `bic_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `bic_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `bic_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `bic_max_days_since_merge`,avg(`c`.`in_degree`) AS `bic_avg_in_degree`,avg(`c`.`out_degree`) AS `bic_avg_out_degree`,avg(`c`.`number_of_branches`) AS `bic_avg_branches`,avg(`c`.`average_degree`) AS `bic_avg_average_degree`,sum(`cm`.`total_additions`) AS `bic_total_additions`,sum(`cm`.`total_deletions`) AS `bic_total_deletions`,sum(`cm`.`total_changes`) AS `bic_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `bic_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `bic_max_changes_in_file`,sum(`cm`.`num_files_changed`) AS `bic_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `bic_change_density_per_file` from (((((`project_pull_commits` `ppc` join `project_pull` `pp` on(((`pp`.`id` = `ppc`.`project_pull_id`) and (`pp`.`project_name` = `ppc`.`project_pull_project_name`) and (`pp`.`project_owner` = `ppc`.`project_pull_project_owner`)))) join `commit` `c` on((`c`.`sha` = `ppc`.`commits_sha`))) join `project_issue_bug_introducing_commits` `pibic` on((`pibic`.`bug_introducing_commits_sha` = `c`.`sha`))) join `project_issue` `pi` on(((`pi`.`id` = `pibic`.`project_issue_id`) and (`pi`.`project_name` = `pibic`.`project_issue_project_name`) and (`pi`.`project_owner` = `pibic`.`project_issue_project_owner`)))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pp`.`state` = 'closed') and (`pp`.`created_at` is not null) and (`pp`.`merged_at` is not null) and (`pi`.`state` = 'closed') and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0) and (`c`.`distance_to_branch_start` > 0) and (`pi`.`project_owner` = 'ansible')) group by `pp`.`project_owner`,`pp`.`project_name`,`pp`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rq2_pr_fix_graph_churn`
--

/*!50001 DROP VIEW IF EXISTS `rq2_pr_fix_graph_churn`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`admin`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq2_pr_fix_graph_churn` AS select `pp`.`project_owner` AS `project_owner`,`pp`.`project_name` AS `project_name`,`pp`.`id` AS `pr_id`,timestampdiff(HOUR,`pp`.`created_at`,`pp`.`merged_at`) AS `pr_review_hours`,count(distinct `c`.`sha`) AS `fix_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `fix_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `fix_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `fix_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `fix_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `fix_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `fix_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `fix_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `fix_max_days_since_merge`,avg(`c`.`in_degree`) AS `fix_avg_in_degree`,avg(`c`.`out_degree`) AS `fix_avg_out_degree`,avg(`c`.`number_of_branches`) AS `fix_avg_branches`,avg(`c`.`average_degree`) AS `fix_avg_average_degree`,sum(`cm`.`total_additions`) AS `fix_total_additions`,sum(`cm`.`total_deletions`) AS `fix_total_deletions`,sum(`cm`.`total_changes`) AS `fix_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `fix_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `fix_max_changes_in_file`,sum(`cm`.`num_files_changed`) AS `fix_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `fix_change_density_per_file` from (((((`project_pull_commits` `ppc` join `project_pull` `pp` on(((`pp`.`id` = `ppc`.`project_pull_id`) and (`pp`.`project_name` = `ppc`.`project_pull_project_name`) and (`pp`.`project_owner` = `ppc`.`project_pull_project_owner`)))) join `commit` `c` on((`c`.`sha` = `ppc`.`commits_sha`))) join `project_issue_fixing_commits` `pifc` on((`pifc`.`fixing_commits_sha` = `c`.`sha`))) join `project_issue` `pi` on(((`pi`.`id` = `pifc`.`project_issue_id`) and (`pi`.`project_name` = `pifc`.`project_issue_project_name`) and (`pi`.`project_owner` = `pifc`.`project_issue_project_owner`)))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pp`.`state` = 'closed') and (`pp`.`created_at` is not null) and (`pp`.`merged_at` is not null) and (`pi`.`state` = 'closed') and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0) and (`pi`.`project_owner` = '123')) group by `pp`.`project_owner`,`pp`.`project_name`,`pp`.`id` */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;

--
-- Final view structure for view `rq3_issue_graph_churn`
--

/*!50001 DROP VIEW IF EXISTS `rq3_issue_graph_churn`*/;
/*!50001 SET @saved_cs_client          = @@character_set_client */;
/*!50001 SET @saved_cs_results         = @@character_set_results */;
/*!50001 SET @saved_col_connection     = @@collation_connection */;
/*!50001 SET character_set_client      = utf8mb4 */;
/*!50001 SET character_set_results     = utf8mb4 */;
/*!50001 SET collation_connection      = utf8mb4_0900_ai_ci */;
/*!50001 CREATE ALGORITHM=UNDEFINED */
/*!50013 DEFINER=`admin`@`%` SQL SECURITY DEFINER */
/*!50001 VIEW `rq3_issue_graph_churn` AS with `fix` as (select `pi`.`project_owner` AS `project_owner`,`pi`.`project_name` AS `project_name`,`pi`.`id` AS `issue_id`,timestampdiff(HOUR,`pi`.`created_at`,`pi`.`closed_at`) AS `issue_resolution_hours`,count(distinct `pifc`.`fixing_commits_sha`) AS `fix_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `fix_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `fix_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `fix_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `fix_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `fix_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `fix_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `fix_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `fix_max_days_since_merge`,avg(`c`.`in_degree`) AS `fix_avg_in_degree`,avg(`c`.`out_degree`) AS `fix_avg_out_degree`,avg(`c`.`number_of_branches`) AS `fix_avg_branches`,avg(`c`.`average_degree`) AS `fix_avg_average_degree`,sum(`cm`.`total_additions`) AS `fix_total_additions`,sum(`cm`.`total_deletions`) AS `fix_total_deletions`,sum(`cm`.`total_changes`) AS `fix_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `fix_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `fix_max_changes_in_file`,avg(`cm`.`num_files_changed`) AS `fix_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `fix_change_density_per_file` from (((`project_issue` `pi` join `project_issue_fixing_commits` `pifc` on(((`pi`.`id` = `pifc`.`project_issue_id`) and (`pi`.`project_name` = `pifc`.`project_issue_project_name`) and (`pi`.`project_owner` = `pifc`.`project_issue_project_owner`)))) join `commit` `c` on((`c`.`sha` = `pifc`.`fixing_commits_sha`))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pi`.`state` = 'closed') and (`pi`.`created_at` is not null) and (`pi`.`closed_at` is not null) and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0)) group by `pi`.`project_owner`,`pi`.`project_name`,`pi`.`id`), `bic` as (select `pi`.`project_owner` AS `project_owner`,`pi`.`project_name` AS `project_name`,`pi`.`id` AS `issue_id`,count(distinct `pibic`.`bug_introducing_commits_sha`) AS `bic_num_commits`,avg(`c`.`min_depth_of_commit_history`) AS `bic_avg_min_depth`,avg(`c`.`max_depth_of_commit_history`) AS `bic_avg_max_depth`,avg(`c`.`distance_to_branch_start`) AS `bic_avg_fp_distance`,max(`c`.`distance_to_branch_start`) AS `bic_max_fp_distance`,avg(`c`.`upstream_heads_unique_on_segment`) AS `bic_avg_upstream_heads`,max(`c`.`upstream_heads_unique_on_segment`) AS `bic_max_upstream_heads`,avg(`c`.`days_since_last_merge_on_segment`) AS `bic_avg_days_since_merge`,max(`c`.`days_since_last_merge_on_segment`) AS `bic_max_days_since_merge`,avg(`c`.`in_degree`) AS `bic_avg_in_degree`,avg(`c`.`out_degree`) AS `bic_avg_out_degree`,avg(`c`.`number_of_branches`) AS `bic_avg_branches`,avg(`c`.`average_degree`) AS `bic_avg_average_degree`,sum(`cm`.`total_additions`) AS `bic_total_additions`,sum(`cm`.`total_deletions`) AS `bic_total_deletions`,sum(`cm`.`total_changes`) AS `bic_total_changes`,avg(`cm`.`avg_changes_per_file`) AS `bic_avg_changes_per_file`,max(`cm`.`max_changes_in_file`) AS `bic_max_changes_in_file`,avg(`cm`.`num_files_changed`) AS `bic_num_files_changed`,(case when (sum(`cm`.`num_files_changed`) > 0) then (sum(`cm`.`total_changes`) / sum(`cm`.`num_files_changed`)) else NULL end) AS `bic_change_density_per_file` from (((`project_issue` `pi` join `project_issue_bug_introducing_commits` `pibic` on(((`pi`.`id` = `pibic`.`project_issue_id`) and (`pi`.`project_name` = `pibic`.`project_issue_project_name`) and (`pi`.`project_owner` = `pibic`.`project_issue_project_owner`)))) join `commit` `c` on((`c`.`sha` = `pibic`.`bug_introducing_commits_sha`))) left join (select `cfc`.`commit_sha` AS `commit_sha`,count(distinct `cfc`.`file_changes_id`) AS `num_files_changed`,sum(`fc`.`total_additions`) AS `total_additions`,sum(`fc`.`total_deletions`) AS `total_deletions`,sum(`fc`.`total_changes`) AS `total_changes`,avg(`fc`.`total_changes`) AS `avg_changes_per_file`,max(`fc`.`total_changes`) AS `max_changes_in_file` from (`commit_file_changes` `cfc` join `file_change` `fc` on((`fc`.`id` = `cfc`.`file_changes_id`))) group by `cfc`.`commit_sha`) `cm` on((`cm`.`commit_sha` = `c`.`sha`))) where ((`pi`.`state` = 'closed') and (`pi`.`created_at` is not null) and (`pi`.`closed_at` is not null) and (`c`.`max_depth_of_commit_history` is not null) and (`c`.`max_depth_of_commit_history` > 0) and (`c`.`distance_to_branch_start` > 0) and (`pi`.`project_owner` = 'ansible') and (`c`.`distance_to_branch_start` > 0)) group by `pi`.`project_owner`,`pi`.`project_name`,`pi`.`id`) select `f`.`project_owner` AS `project_owner`,`f`.`project_name` AS `project_name`,`f`.`issue_id` AS `issue_id`,`f`.`issue_resolution_hours` AS `issue_resolution_hours`,`f`.`fix_num_commits` AS `fix_num_commits`,`f`.`fix_avg_min_depth` AS `fix_avg_min_depth`,`f`.`fix_avg_max_depth` AS `fix_avg_max_depth`,`f`.`fix_avg_fp_distance` AS `fix_avg_fp_distance`,`f`.`fix_max_fp_distance` AS `fix_max_fp_distance`,`f`.`fix_avg_upstream_heads` AS `fix_avg_upstream_heads`,`f`.`fix_max_upstream_heads` AS `fix_max_upstream_heads`,`f`.`fix_avg_days_since_merge` AS `fix_avg_days_since_merge`,`f`.`fix_max_days_since_merge` AS `fix_max_days_since_merge`,`f`.`fix_avg_in_degree` AS `fix_avg_in_degree`,`f`.`fix_avg_out_degree` AS `fix_avg_out_degree`,`f`.`fix_avg_branches` AS `fix_avg_branches`,`f`.`fix_avg_average_degree` AS `fix_avg_average_degree`,`f`.`fix_total_additions` AS `fix_total_additions`,`f`.`fix_total_deletions` AS `fix_total_deletions`,`f`.`fix_total_changes` AS `fix_total_changes`,`f`.`fix_avg_changes_per_file` AS `fix_avg_changes_per_file`,`f`.`fix_max_changes_in_file` AS `fix_max_changes_in_file`,`f`.`fix_num_files_changed` AS `fix_num_files_changed`,`f`.`fix_change_density_per_file` AS `fix_change_density_per_file`,`b`.`bic_num_commits` AS `bic_num_commits`,`b`.`bic_avg_min_depth` AS `bic_avg_min_depth`,`b`.`bic_avg_max_depth` AS `bic_avg_max_depth`,`b`.`bic_avg_fp_distance` AS `bic_avg_fp_distance`,`b`.`bic_max_fp_distance` AS `bic_max_fp_distance`,`b`.`bic_avg_upstream_heads` AS `bic_avg_upstream_heads`,`b`.`bic_max_upstream_heads` AS `bic_max_upstream_heads`,`b`.`bic_avg_days_since_merge` AS `bic_avg_days_since_merge`,`b`.`bic_max_days_since_merge` AS `bic_max_days_since_merge`,`b`.`bic_avg_in_degree` AS `bic_avg_in_degree`,`b`.`bic_avg_out_degree` AS `bic_avg_out_degree`,`b`.`bic_avg_branches` AS `bic_avg_branches`,`b`.`bic_avg_average_degree` AS `bic_avg_average_degree`,`b`.`bic_total_additions` AS `bic_total_additions`,`b`.`bic_total_deletions` AS `bic_total_deletions`,`b`.`bic_total_changes` AS `bic_total_changes`,`b`.`bic_avg_changes_per_file` AS `bic_avg_changes_per_file`,`b`.`bic_max_changes_in_file` AS `bic_max_changes_in_file`,`b`.`bic_num_files_changed` AS `bic_num_files_changed`,`b`.`bic_change_density_per_file` AS `bic_change_density_per_file` from (`fix` `f` left join `bic` `b` on(((`f`.`project_owner` = `b`.`project_owner`) and (`f`.`project_name` = `b`.`project_name`) and (`f`.`issue_id` = `b`.`issue_id`)))) */;
/*!50001 SET character_set_client      = @saved_cs_client */;
/*!50001 SET character_set_results     = @saved_cs_results */;
/*!50001 SET collation_connection      = @saved_col_connection */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-07-04 10:47:02

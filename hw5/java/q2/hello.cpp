#include <iostream>
#include <mpi.h>
#include <fstream>
#include <string>
#include <stdlib.h>
#include <sstream>
#include <vector>

#define VECTOR      0
#define COL_COUNT   1
#define ROW_COUNT   2
#define SOME_ROWS   3
#define RESULT      4

#define SOURCE_PROCESS 0


using namespace std;


std::vector<string> readMatrixFile();
string readVectorFile();
void logMatrix(std::vector<string> matrix);
std::vector<int> arrayElemAt(string vector);
void logVectorArray(const std::vector<int> &vector_array);
std::vector< std::vector<int> > convertMatrixToArr(const std::vector<string> matrix);
void logMatrixArr(std::vector< std::vector<int> > matrix_arr);
void sendVectorArrayToAll(std::vector<int> vector, int num_processes);
void sendVectorArrayWhere(const std::vector<int> &vector, int msgTag, int process_num);
void sendMatrixArrayToSome(std::vector<std::vector<int> > matrix, int num_rows, int processes);
void sendSomeRowsTo(std::vector< std::vector<int> > matrix, int rows_per_proc, int row, int process);
void sendMatrixRowsMoreThanProc(std::vector< std::vector<int> > matrix, int rows, int processes);
void sendCount(int count, int msgTag, int process);
int receiveCount(int msgTag, int source);


void receiveVectorArray(int* array, int msgTag, int source);

/**--------------------------------------------------------------------------------------*/
int main(int argc, char **argv) {
	// Initialize the MPI environment
	MPI_Init(NULL, NULL);

	// Get the number of processes
	int num_processes;
	MPI_Comm_size(MPI_COMM_WORLD, &num_processes);

	// Get the rank of the process
	int cur_process_rank;
	MPI_Comm_rank(MPI_COMM_WORLD, &cur_process_rank);

	//distribute rows
	if(cur_process_rank == 0) {
		std::vector<string> matrix_str = readMatrixFile();
		string              vector_str = readVectorFile();
		int                 num_rows = atoi(matrix_str[0].c_str());
		int                 rows_per_process;

		matrix_str.erase(matrix_str.begin());   //remove first row (number of total rows)


		std::vector<int>                vector_arr = arrayElemAt(vector_str);
		std::vector< std::vector<int> > matrix_arr = convertMatrixToArr(matrix_str);


		//sending vector...
		sendVectorArrayToAll(vector_arr, num_processes);

		//distributing rows...
		if(num_processes-1 >= num_rows) sendMatrixArrayToSome       (matrix_arr, num_rows, num_processes);
		else                            sendMatrixRowsMoreThanProc  (matrix_arr, num_rows, num_processes);
	}


	if(cur_process_rank != 0) {
		//receive rows
		int col_count = receiveCount(COL_COUNT, SOURCE_PROCESS);
		cout << "(Process " << cur_process_rank << ") col count is: " << col_count << endl;

		int vector_arr[col_count];

		receiveVectorArray(vector_arr, VECTOR, SOURCE_PROCESS);


	}

	// Finalize the MPI environment.
	MPI_Finalize();
	/*

	// Get the name of the processor
	char processor_name[MPI_MAX_PROCESSOR_NAME];
	int  name_len;
	MPI_Get_processor_name(processor_name, &name_len);
*/
}

void receiveVectorArray(int* array, int msgTag, int source) {
	MPI_Status status;

	MPI_Recv(array,
	         1,
	         MPI_INT,
	         source,
	         msgTag,
	         MPI_COMM_WORLD,
	         &status
	);
}

int receiveCount(int msgTag, int source) {
	int buf;
	MPI_Status status;

	MPI_Recv(&buf, 1, MPI_INT, source, msgTag, MPI_COMM_WORLD, &status);

	return buf;
}

std::vector< std::vector<int> > convertMatrixToArr(const std::vector<string> matrix) {
	std::vector< std::vector<int> > result;

	for(int i = 0; i < matrix.size(); ++i) {
		result.push_back(arrayElemAt(matrix[i]));
	}

	return result;
}

std::vector<int> arrayElemAt(const string vector) {
	stringstream char_stream;
	char_stream << vector;

	std::vector<int> vector_array;


	for(int i = 0; i < vector.size(); ++i) {
		if (char_stream.peek() == EOF || char_stream.peek() == '\n') break;

		int v;
		char_stream >> v;
		vector_array.push_back(v);
	}


	return vector_array;
}

void logVectorArray(const std::vector<int> &vector_array) {
	cout << "vector size: " << vector_array.size() << endl;

	for(int j = 0; j < vector_array.size(); ++j)
		cout << vector_array[j] << " ";

	cout << endl;
}

void sendSomeRowsTo(std::vector< std::vector<int> > matrix, int rows_per_proc, int row, int process){
	for(int i = row; i < (row+rows_per_proc); ++i)
		sendVectorArrayWhere(matrix[i], SOME_ROWS, process);
}

void sendCount(int count, int msgTag, int process) {
	int buff_size = 1;

	MPI_Send(&count,
	         buff_size,
	         MPI_INT,
	         process,
	         msgTag,
	         MPI_COMM_WORLD
	);
}

void sendVectorArrayToAll(std::vector<int> vector, int num_processes) {
	int col_count = static_cast<int>(vector.size());

	for(int cur_process = 1; cur_process < num_processes; ++cur_process) {
		cout << "sending column count to process " << cur_process << endl;
		sendCount(col_count, COL_COUNT, cur_process);
		sendVectorArrayWhere(vector, VECTOR, cur_process);
	}
}

void sendVectorArrayWhere(const std::vector<int> &vector, int msgTag, int process_num) {
	MPI_Send(vector.data(),
	         static_cast<int>(vector.size()),
	         MPI_INT,
	         process_num,
	         msgTag,
	         MPI_COMM_WORLD
	);
}

void sendMatrixRowsMoreThanProc(std::vector< std::vector<int> > matrix, int rows, int processes) {
	int proc_excluding_src  = processes-1;
	int rows_per_proc       = rows / proc_excluding_src;
	int remainder           = rows % proc_excluding_src;
	int row                 = 0;


	for(int cur_process = 1; cur_process < processes; cur_process++) {
		int row_count = cur_process != processes-1 ? rows_per_proc : rows_per_proc + remainder;

		sendCount(row_count, ROW_COUNT, cur_process);
		sendSomeRowsTo(matrix, rows_per_proc, row, cur_process);

		row += rows_per_proc;
	}

	if (remainder != 0) sendSomeRowsTo(matrix, remainder, row, processes-1);

}

void logMatrixArr(std::vector< std::vector<int> > matrix_arr) {

	for(int i = 0; i < matrix_arr.size(); ++i) {
		logVectorArray(matrix_arr[i]);
	}
}

void sendMatrixArrayToSome(std::vector<std::vector<int> > matrix, int num_rows, int processes) {
	int cur_process = 1;
	int rows_per_process = 1;

	for(int row = 1; row <= num_rows; row++, cur_process++) {
		sendCount(rows_per_process, ROW_COUNT, cur_process);
		sendVectorArrayWhere(matrix[row], SOME_ROWS, cur_process);
	}

	//notify the rest of processes that they will not receive any rows!
	while (cur_process < processes-1){
		sendCount(0, ROW_COUNT, cur_process);
		cur_process++;
	}


}

void logMatrix(std::vector<string> matrix) {
	for(int i = 0; i < matrix.size(); ++i) cout<<matrix.at(i)<<endl;
	cout<<"----------------"<<endl;
}

std::vector<string> readMatrixFile() {
	ifstream matrix_file;
	matrix_file.open("Matrix.txt");

	string         line;
	std::vector<string> matrix_rows;

	if(matrix_file.is_open()) {
		while(getline(matrix_file, line)) {
			matrix_rows.push_back(line);
		}
		matrix_file.close();
	}
	else cout<<"Unable to open file"<<endl;

//	for(int i = 0; i < matrix_rows.size(); ++i) {
//		cout << matrix_rows.at(i) << endl;
//	}

	return matrix_rows;

}

string readVectorFile() {
	ifstream vector_file;
	vector_file.open("Vector.txt");

	string vector;

	if(vector_file.is_open()) {
		getline(vector_file, vector);

		vector_file.close();
	}
	else cout<<"Unable to open file"<<endl;


//	cout << vector << endl;
	vector_file.close();

	return vector;

}
/*********************

888888ba  888888ba  .d88888b                    
88    `8b 88    `8b 88.    "'    dP       dP    
88     88 88     88 `Y88888b.    88       88    
88     88 88     88       `8b 88888888 88888888 
88    .8P 88     88 d8'   .8P    88       88    
8888888P  dP     dP  Y88888P     dP       dP    
                                                
*********************/

#include<stdio.h>
#include<string.h>

#define NPUBS 10
#define NSUBS 10
#define NBROKERS 100
#define PUB 0
#define SUB 0
#define MAX_LINE_LENGTH 100

/* Global variables */
int total_messages;
int root=99;
int debug=1;
int nbrokers;

/* Struct definitions */

struct pub{
int x, y, z;// coordinates
int broker;
int message_counter;
} pubs[NPUBS];

struct sub{
int x,y,z; // coordinates 
int broker;
int message_counter;
}subs[NSUBS];

struct broker{
int x1,y1,z1; // coordinates corner 1
int x2, y2, z2;  // coordinates corner 2
int do_i_subscribe; // bollean if a brokers subscribes
int sub_x1, sub_x2, sub_y1, sub_y2; /* Coordinates of my subscription rectangule*/
int parent;
int nsubs;
int *subs; // dynamic array. 
int nchildren;
int *children;
int pubs_counter;
int subs_counter;
} brokers[NBROKERS];

/* Return 1 if rectangule changed, 0 if not for now */
int calculate_overlap(int x1_1, int x1_2, int x2_1, int x2_2,...)
 {
 }

/*****************************************************
Prints statistics to the screen
*****************************************************/
void print_stats()
 {
  int i;
  // Print for every broker
   for(i=0;i<nbrokers;i++)
    {
     printf("%d %d %d\n",i,brokers[i].pubs_counter, brokers[i].subs_counter);
    }

  // Print total
     printf("Total=%d\n",total_messages);
 }

/*****************************************************
Loads the topology to memory (maybe from a file)
*****************************************************/
void create_topology()
 {
  /* Create brokers */

 }

/*****************************************************
Implements the trasnmission of a publication through the overlay
*****************************************************/
void traverse_pub(int broker, int x, int y)
  {
   int i;

   // update stats

   // first we process down
   for(i=0; i<brokers[broker].nchildren;i++)
     {
       traverse_pub(brokers[broker].children[i]);
     }
   // if broker is a leaf let's give it to the subscribers
   for(i=0; i<brokers[broker].nsubs;i++)
     {
      // Increase message of the subscriber
      subs[brokers[broker].subs[i]].message_counter++;
     }
   // now we  process up 
   if(broker==root)
     return;
    else traverse_pub(brokers[broker].parent);

  }

/*****************************************************
Implements the trasnmission of a subscription through the overlay
*****************************************************/
void traverse_sub(int src,int broker, int x1, int x2, int y1, int y2)
 {
   // FIXME: If I have a subscription check for overlap. If rectangule changed:
   //  - Update my subscription
   //  - Send if up as well
   // broker now subscribes
      brokers[broker].do_i_subscribe=1;
   // Update subscription table
      if (src!=-1)//Im not a leaf
       {
        brokers[broker].subs[brokers[broker].nsubs++]=src;
       }
   // now we  process up 
   if(broker==root)
     return;
    else traverse_sub(broker,brokers[broker].parent);
 }

/*****************************************************
main function iterates through event list.
For each event simulates either a publication or subscription
At each broker and subscriber, message counters are updated
*****************************************************/
int main(int argc, char *argv[])
{
char line[255];
FILE *file;
int event_type;
int event_node;
int count=0;

// check argv for simulation parameters. Eg/david of Miguel modus operandi

// create topology (possibly reading from file)
  create_topology();

// read events file/f

    // Open file
    file = fopen("events.txt", "r");
    if (file == NULL) {
        printf("Error: Could not open file events.txt \n");
        return -1;
    }
    
    // Read file line by line
    while (fgets(line, MAX_LINE_LENGTH, file) != NULL ) {
        char token[4];
        int number;
        
        // Parse the line: expecting format "PUB/SUB number"
        if (sscanf(line, "%3s %d", token, &number) != 2)
          {
            printf("Error: Invalid line format at line %d\n", count + 1);
            fclose(file);
            return -1;
          }
        
        // Convert PUB/SUB to integer and store
        if (strcmp(token, "PUB") == 0)
           {
            traverse_pub(event_node);
           }
         else if (strcmp(token, "SUB") == 0)
               { // FIX ME: Deal with subscriber first 
                traverse_sub(-1,event_node,int x1, int x2, int y1, int y2);
               }
               else 
                  {
                   printf("Error: Invalid token '%s' at line %d\n", token, count + 1);
                  }
        }
// print stats
print_stats();
}


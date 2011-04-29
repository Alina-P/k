#include <stdlib.h>
#include <stdio.h>


struct listNode {
  int val;
  struct listNode *next;
};


struct listNode* append(struct listNode *x, int i)
//@ rule <k> $ => return ?x; </k> <heap_> list(x)(A) => list(?x)([i] @ A) <_/heap>
{
  struct listNode *p;
  p = (struct listNode*)malloc(sizeof(struct listNode));
  p->val = i;
  
  if (x == 0)
  { 
    p->next = 0;
  }
  else
  {
    p->next = x;
  }
  x = p;
  return x;
}

struct listNode* create(int n)
{
	struct listNode *x;
	struct listNode *y;
	x = 0;
	while (n)
	{
		y = x;
		x = (struct listNode*)malloc(sizeof(struct listNode));
		x->val = n;
		x->next = y;
		n -= 1;
	}
	return x;
}

void destroy(struct listNode* x)
//@ rule <k> $ => return; </k><heap_> list(x)(A) => . <_/heap>
{
	//@ inv <heap_> list(x)(?A) <_/heap>
	while(x)
	{
		struct listNode *y;
		
		y = x->next;
		free(x);
		x = y;
	}
}


void print(struct listNode* x)
/*@ rule <k> $ => return; </k>
 <heap_> list(x)(A) <_/heap>
 <out_> epsilon => A </out> */
{
	/*@ inv <heap_> lseg(old(x), x)(?A1), list(x)(?A2) <_/heap> <out_> ?A1 </out>
	 /\ A = ?A1 @ ?A2 */
	while(x)
	{
		printf("%d ",x->val);
		x = x->next;
	}
	printf("\n"); 
}


int main()
{
  struct listNode *x;
  struct listNode *y;

  x = create(5);
  //@ assert <heap> list(x)([1, 2, 3, 4, 5]) </heap>
  x = append(x,15);
  //@ assert <heap> list(x)([15, 1, 2, 3, 4, 5]) </heap>
  destroy(x);
  //@ assert <heap> . </heap>
  
  return 0;
}


//@ var n, i : Int
//@ var A : Seq

